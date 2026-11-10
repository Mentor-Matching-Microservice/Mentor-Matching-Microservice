package com.mentormatching.notification.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.UUID;

import com.mentormatching.notification.event.InvalidEventException;
import com.mentormatching.notification.support.FakeBookingService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class BookingParticipantsClientTest {

    private static final FakeBookingService BOOKINGS = new FakeBookingService();

    private final BookingParticipantsClient client = new BookingParticipantsClient(
            new BookingClientProperties(BOOKINGS.baseUrl() + "/", Duration.ofSeconds(1), Duration.ofSeconds(2)),
            JsonMapper.builder().build());

    @BeforeEach
    void reset() {
        BOOKINGS.reset();
    }

    @AfterAll
    static void stop() {
        // Server dùng chung trong JVM test; để JVM tự dọn.
    }

    @Test
    void readsLearnerAndMentorAccountsFromTheBookingServiceResponse() {
        UUID learner = UUID.randomUUID();
        UUID mentor = UUID.randomUUID();
        BOOKINGS.booking("42", learner, mentor);

        BookingParticipants participants = client.find("42");

        assertThat(participants.learnerUserId()).isEqualTo(learner);
        assertThat(participants.mentorUserId()).isEqualTo(mentor);
    }

    @Test
    void missingBookingIsAPermanentError() {
        assertThatThrownBy(() -> client.find("404")).isInstanceOf(InvalidEventException.class)
                .hasMessage("Booking not found");
    }

    @Test
    void serverErrorsAndThrottlingAreTransient() {
        BOOKINGS.failWith("1", 503);
        BOOKINGS.failWith("2", 500);
        BOOKINGS.failWith("3", 429);
        assertThatThrownBy(() -> client.find("1")).isInstanceOf(RecipientLookupException.class);
        assertThatThrownBy(() -> client.find("2")).isInstanceOf(RecipientLookupException.class);
        assertThatThrownBy(() -> client.find("3")).isInstanceOf(RecipientLookupException.class);
    }

    @Test
    void unreachableBookingServiceIsTransient() {
        BookingParticipantsClient down = new BookingParticipantsClient(
                new BookingClientProperties("http://localhost:1", Duration.ofMillis(300), Duration.ofMillis(300)),
                JsonMapper.builder().build());
        assertThatThrownBy(() -> down.find("1")).isInstanceOf(RecipientLookupException.class);
    }

    @Test
    void nonNumericBookingIdsAreRejectedBeforeAnyCall() {
        assertThatThrownBy(() -> client.find("../admin")).isInstanceOf(InvalidEventException.class);
        assertThatThrownBy(() -> client.find("1 OR 1=1")).isInstanceOf(InvalidEventException.class);
        assertThatThrownBy(() -> client.find(null)).isInstanceOf(InvalidEventException.class);
        assertThat(BOOKINGS.requestCount()).isZero();
    }

    @Test
    void propertiesDefaultsAndTrailingSlash() {
        BookingClientProperties defaults = new BookingClientProperties(null, null, null);
        assertThat(defaults.baseUrl()).isEqualTo("http://localhost:8084");
        assertThat(defaults.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(new BookingClientProperties("http://b:1/", null, null).baseUrl()).isEqualTo("http://b:1");
    }
}
