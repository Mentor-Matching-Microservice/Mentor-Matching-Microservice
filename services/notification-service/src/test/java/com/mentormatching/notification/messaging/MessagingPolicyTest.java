package com.mentormatching.notification.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.mentormatching.notification.event.InvalidEventException;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.AmqpException;
import org.springframework.dao.DataAccessResourceFailureException;

class MessagingPolicyTest {

    @Test
    void invalidEventsAreNeverRetriedEvenWhenWrapped() {
        assertThat(MessagingConfiguration.isRetryable(new InvalidEventException("x"))).isFalse();
        assertThat(MessagingConfiguration.isRetryable(
                new AmqpException("listener failed", new InvalidEventException("x")))).isFalse();
        assertThat(MessagingConfiguration.isRetryable(new AmqpRejectAndDontRequeueException("x"))).isFalse();
    }

    @Test
    void transientFailuresAreRetried() {
        assertThat(MessagingConfiguration.isRetryable(new AmqpException("listener failed",
                new DataAccessResourceFailureException("mongo down")))).isTrue();
    }

    @Test
    void propertiesDefaultToTheSharedExchangeAndAPrivateQueue() {
        MessagingProperties defaults = new MessagingProperties(null, null, null, null, null);
        assertThat(defaults.enabled()).isTrue();
        assertThat(defaults.exchange()).isEqualTo("mentor.events");
        assertThat(defaults.queue()).isEqualTo("notification.domain-events");
        assertThat(defaults.routingKeys()).containsExactly("auth.#", "mentor.#", "booking.#", "payment.#", "review.#");
        assertThat(defaults.deadLetterExchange()).isEqualTo("notification.domain-events.dlx");
        assertThat(defaults.deadLetterQueue()).isEqualTo("notification.domain-events.dead");
        assertThat(defaults.maxRetries()).isEqualTo(4);
    }

    @Test
    void retryCountIsBounded() {
        assertThatThrownBy(() -> new MessagingProperties(true, "x", "q", List.of("a"), 11))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MessagingProperties(true, "x", "q", List.of("a"), -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
