package com.mentormatching.payment.service;

import java.util.concurrent.TimeUnit;
import com.mentormatching.payment.domain.OutboxEvent;
import com.mentormatching.payment.domain.OutboxEventRepository;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "payment.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentOutboxPublisher {
    private final OutboxEventRepository events;
    private final RabbitTemplate rabbit;
    private final String exchange;

    public PaymentOutboxPublisher(OutboxEventRepository events, RabbitTemplate rabbit,
                                  @Value("${payment.messaging.exchange:mentor.events}") String exchange) {
        this.events = events;
        this.rabbit = rabbit;
        this.exchange = exchange;
    }

    @Scheduled(fixedDelayString = "${PAYMENT_OUTBOX_PUBLISH_DELAY_MS:5000}")
    public void publishPending() {
        for (OutboxEvent event : events.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            publish(event);
        }
    }

    private void publish(OutboxEvent event) {
        try {
            CorrelationData correlation = new CorrelationData(event.getId().toString());
            rabbit.convertAndSend(exchange, "payment.events", event.getPayload(), message -> {
                message.getMessageProperties().setMessageId(event.getId().toString());
                message.getMessageProperties().setContentType("application/json");
                return message;
            }, correlation);
            CorrelationData.Confirm confirm = correlation.getFuture().get(10, TimeUnit.SECONDS);
            if (!confirm.isAck()) {
                throw new IllegalStateException("RabbitMQ did not confirm event: " + confirm.getReason());
            }
            if (correlation.getReturned() != null) {
                throw new IllegalStateException("RabbitMQ returned an unroutable payment event");
            }
            event.published();
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            event.failed(ex.getMessage());
        }
        events.save(event);
    }
}
