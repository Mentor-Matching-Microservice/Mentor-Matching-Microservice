package com.mentormatching.notification.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.mentormatching.notification.client.BookingClientProperties;
import com.mentormatching.notification.client.BookingParticipants;
import com.mentormatching.notification.client.BookingParticipantsClient;
import com.mentormatching.notification.domain.NotificationType;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** Ánh xạ người nhận và nội dung cho sự kiện booking/payment/review (#81). */
class DomainEventMappers81Test {

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final UUID learner = UUID.randomUUID();
    private final UUID mentor = UUID.randomUUID();
    private final AtomicInteger lookups = new AtomicInteger();
    private final BookingParticipantsClient bookings = new BookingParticipantsClient(
            new BookingClientProperties("http://unused", null, null), mapper) {
        @Override
        public BookingParticipants find(String bookingId) {
            lookups.incrementAndGet();
            return new BookingParticipants(bookingId, learner, mentor);
        }
    };
    private final BookingEventMapper booking = new BookingEventMapper(bookings);

    @Test
    void recipientsPerBookingEventMatchIssue81() {
        assertThat(recipients("BookingCreated")).containsExactly(mentor);
        assertThat(recipients("BookingConfirmed")).containsExactly(learner, mentor);
        assertThat(recipients("BookingRejected")).containsExactly(learner);
        assertThat(recipients("BookingCancelled")).containsExactly(learner, mentor);
        assertThat(recipients("BookingConfirmationFailed")).containsExactly(learner);
        assertThat(recipients("BookingCompleted")).containsExactly(learner, mentor);
    }

    @Test
    void bookingEventUsesParticipantsFromPayloadWhenPresent() {
        List<NotificationDraft> drafts = booking.map(parse("""
                {"eventId":"e","eventType":"BookingCreated","bookingId":5,
                 "studentUserId":"%s","mentorUserId":"%s"}""".formatted(learner, mentor)));
        assertThat(drafts).singleElement().satisfies(d -> assertThat(d.recipientUserId()).isEqualTo(mentor));
        assertThat(lookups).hasValue(0);
    }

    @Test
    void bookingEventWithoutBookingIdIsInvalid() {
        assertThatThrownBy(() -> booking.map(parse("{\"eventId\":\"e\",\"eventType\":\"BookingConfirmed\"}")))
                .isInstanceOf(InvalidEventException.class).hasMessageContaining("bookingId");
    }

    @Test
    void cancellationReasonIsShownAndTruncated() {
        String longReason = "x".repeat(1000);
        NotificationDraft draft = booking.map(parse("""
                {"eventId":"e","eventType":"BookingCancelled","bookingId":"9","reason":"%s"}""".formatted(longReason)))
                .getFirst();
        assertThat(draft.type()).isEqualTo(NotificationType.BOOKING_CANCELLED);
        assertThat(draft.message()).contains("Lý do: xxx").hasSizeLessThan(450);
    }

    @Test
    void paymentEventsTargetThePayer() {
        PaymentEventMapper payment = new PaymentEventMapper();
        NotificationDraft ok = payment.map(parse("""
                {"eventId":"e","eventType":"PaymentSucceeded","paymentId":"p1","payerUserId":"%s",
                 "amount":1250000.00,"currency":"vnd"}""".formatted(learner))).getFirst();
        assertThat(ok.recipientUserId()).isEqualTo(learner);
        assertThat(ok.type()).isEqualTo(NotificationType.PAYMENT_SUCCESS);
        assertThat(ok.message()).contains("1.250.000 VND");
        assertThat(ok.referenceId()).isEqualTo("p1");

        NotificationDraft refunded = payment.map(parse("""
                {"eventId":"e","eventType":"PaymentRefunded","payerUserId":"%s","amount":500000,
                 "refundedAmount":200000,"currency":"VND","reason":"Mentor huỷ"}""".formatted(learner))).getFirst();
        assertThat(refunded.type()).isEqualTo(NotificationType.PAYMENT_REFUNDED);
        assertThat(refunded.message()).contains("200.000 VND").contains("Mentor huỷ");

        NotificationDraft failed = payment.map(parse("""
                {"eventId":"e","eventType":"PaymentFailed","payerUserId":"%s","reason":"Checkout session expired"}
                """.formatted(learner))).getFirst();
        assertThat(failed.type()).isEqualTo(NotificationType.PAYMENT_FAILED);

        assertThatThrownBy(() -> payment.map(parse("{\"eventId\":\"e\",\"eventType\":\"PaymentFailed\"}")))
                .isInstanceOf(InvalidEventException.class);
    }

    @Test
    void reviewCreatedTargetsTheMentorAccount() {
        ReviewEventMapper review = new ReviewEventMapper(bookings);
        NotificationDraft draft = review.map(parse("""
                {"eventId":"e","eventType":"ReviewCreated","reviewId":3,"bookingId":8,
                 "studentUserId":"%s","mentorId":7,"rating":4}""".formatted(learner))).getFirst();
        assertThat(draft.recipientUserId()).isEqualTo(mentor);
        assertThat(draft.type()).isEqualTo(NotificationType.REVIEW_CREATED);
        assertThat(draft.message()).contains("4★");
        assertThat(draft.referenceId()).isEqualTo("3");
        assertThat(lookups).hasValue(1);

        NotificationDraft weird = review.map(parse("""
                {"eventId":"e","eventType":"ReviewCreated","bookingId":8,"rating":"<b>9</b>"}""")).getFirst();
        assertThat(weird.message()).doesNotContain("<b>").doesNotContain("★");
    }

    @Test
    void moneyFormattingFollowsVietnameseConventions() {
        assertThat(Texts.money(new BigDecimal("500000"), "VND")).isEqualTo("500.000 VND");
        assertThat(Texts.money(new BigDecimal("12.5"), "usd")).isEqualTo("12,5 USD");
        assertThat(Texts.money(null, "VND")).isNull();
    }

    private List<UUID> recipients(String type) {
        return booking.map(parse("{\"eventId\":\"e\",\"eventType\":\"" + type + "\",\"bookingId\":\"1\"}"))
                .stream().map(NotificationDraft::recipientUserId).toList();
    }

    private DomainEvent parse(String json) {
        return DomainEvent.parse(json.strip().getBytes(StandardCharsets.UTF_8), mapper);
    }
}
