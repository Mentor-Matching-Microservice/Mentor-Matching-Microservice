package com.mentormatching.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mentormatching.notification.domain.DeliveryAttempt;
import com.mentormatching.notification.domain.NotificationType;
import com.mentormatching.notification.dto.NotificationResponse;
import com.mentormatching.notification.repository.DeliveryAttemptRepository;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class RealtimeNotificationPublisherTest {

    private final SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
    private final DeliveryAttemptRepository repository = mock(DeliveryAttemptRepository.class);
    private final List<DeliveryAttempt> recorded = new ArrayList<>();
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-10T10:00:00Z"), ZoneOffset.UTC);
    private final NotificationResponse notification = new NotificationResponse(UUID.randomUUID().toString(),
            UUID.randomUUID().toString(), NotificationType.SYSTEM, "t", "m", false, null, null, Instant.now(clock), null);

    RealtimeNotificationPublisherTest() {
        when(repository.insert(any(DeliveryAttempt.class))).thenAnswer(inv -> {
            recorded.add(inv.getArgument(0));
            return inv.getArgument(0);
        });
    }

    @Test
    void firstSuccessfulAttemptIsRecorded() {
        assertThat(publisher(3).deliver(notification)).isTrue();

        assertThat(recorded).singleElement().satisfies(a -> {
            assertThat(a.getOutcome()).isEqualTo(DeliveryAttempt.Outcome.SENT);
            assertThat(a.getAttempt()).isEqualTo(1);
            assertThat(a.getNotificationId()).isEqualTo(notification.id());
            assertThat(a.getChannel()).isEqualTo(DeliveryAttempt.Channel.WEBSOCKET);
        });
    }

    @Test
    void transientFailuresAreRetriedAndEveryAttemptIsRecorded() {
        doThrow(new MessageDeliveryException("broker busy"))
                .doThrow(new MessageDeliveryException("broker busy"))
                .doNothing()
                .when(template).convertAndSendToUser(anyString(), anyString(), any(Object.class));

        assertThat(publisher(3).deliver(notification)).isTrue();

        assertThat(recorded).extracting(DeliveryAttempt::getOutcome).containsExactly(
                DeliveryAttempt.Outcome.FAILED, DeliveryAttempt.Outcome.FAILED, DeliveryAttempt.Outcome.SENT);
        assertThat(recorded).extracting(DeliveryAttempt::getAttempt).containsExactly(1, 2, 3);
        assertThat(recorded.getFirst().getError()).isEqualTo("MessageDeliveryException");
    }

    @Test
    void retriesAreBoundedAndNeverThrow() {
        doThrow(new MessageDeliveryException("down"))
                .when(template).convertAndSendToUser(anyString(), anyString(), any(Object.class));

        assertThat(publisher(2).deliver(notification)).isFalse();

        verify(template, times(2)).convertAndSendToUser(eq(notification.userId()), anyString(), any(Object.class));
        assertThat(recorded).extracting(DeliveryAttempt::getOutcome)
                .containsExactly(DeliveryAttempt.Outcome.FAILED, DeliveryAttempt.Outcome.FAILED);
    }

    @Test
    void failingToRecordAnAttemptDoesNotBreakDelivery() {
        when(repository.insert(any(DeliveryAttempt.class))).thenThrow(new IllegalStateException("mongo down"));
        doNothing().when(template).convertAndSendToUser(anyString(), anyString(), any(Object.class));

        assertThat(publisher(3).deliver(notification)).isTrue();
    }

    @Test
    void propertiesAreBounded() {
        assertThatThrownBy(() -> new DeliveryProperties(0, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DeliveryProperties(6, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DeliveryProperties(3, Duration.ofSeconds(10)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new DeliveryProperties(null, null).maxAttempts()).isEqualTo(3);
    }

    private RealtimeNotificationPublisher publisher(int maxAttempts) {
        return new RealtimeNotificationPublisher(template, repository,
                new DeliveryProperties(maxAttempts, Duration.ofMillis(1)), clock);
    }
}
