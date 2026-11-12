package com.mentormatching.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import com.mentormatching.payment.domain.OutboxEvent;
import com.mentormatching.payment.domain.OutboxEventRepository;
import com.mentormatching.payment.service.PaymentOutboxPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class PaymentOutboxPublisherTest {
    @Mock OutboxEventRepository events;
    @Mock RabbitTemplate rabbit;

    @Test
    void marksOutboxPublishedOnlyAfterBrokerAcknowledges() {
        OutboxEvent event = new OutboxEvent(UUID.randomUUID(), "PaymentSucceeded", UUID.randomUUID(), "{\"eventId\":\"x\"}");
        when(events.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(event));
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(4);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, "accepted"));
            return null;
        }).when(rabbit).convertAndSend(eq("mentor.events"), eq("payment.events"), eq(event.getPayload()),
                any(MessagePostProcessor.class), any(CorrelationData.class));

        new PaymentOutboxPublisher(events, rabbit, "mentor.events").publishPending();

        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(event.getPublishAttempts()).isZero();
        verify(events).save(event);
    }

    @Test
    void keepsOutboxPendingWhenBrokerNacks() {
        OutboxEvent event = new OutboxEvent(UUID.randomUUID(), "PaymentFailed", UUID.randomUUID(), "{\"eventId\":\"y\"}");
        when(events.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(event));
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(4);
            correlation.getFuture().complete(new CorrelationData.Confirm(false, "broker rejected"));
            return null;
        }).when(rabbit).convertAndSend(eq("mentor.events"), eq("payment.events"), eq(event.getPayload()),
                any(MessagePostProcessor.class), any(CorrelationData.class));

        new PaymentOutboxPublisher(events, rabbit, "mentor.events").publishPending();

        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getPublishAttempts()).isEqualTo(1);
        assertThat(event.getLastError()).contains("RabbitMQ did not confirm");
        verify(events).save(event);
    }
}
