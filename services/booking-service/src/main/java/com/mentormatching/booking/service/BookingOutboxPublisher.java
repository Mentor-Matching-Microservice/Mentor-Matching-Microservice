package com.mentormatching.booking.service;

import com.mentormatching.booking.entity.OutboxEvent;
import com.mentormatching.booking.repository.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "booking.messaging.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class BookingOutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public BookingOutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            RabbitTemplate rabbitTemplate,
            @Value("${booking.messaging.exchange:mentor.events}") String exchange,
            @Value("${booking.messaging.booking-lifecycle-routing-key:booking.lifecycle}") String routingKey) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    @Scheduled(fixedDelayString = "${BOOKING_OUTBOX_PUBLISH_DELAY_MS:5000}")
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
        for (OutboxEvent event : pending) {
            publish(event);
        }
    }

    private void publish(OutboxEvent event) {
        try {
            CorrelationData correlation = new CorrelationData(event.getId().toString());
            rabbitTemplate.convertAndSend(exchange, routingKey, event.getPayload(), message -> {
                message.getMessageProperties().setMessageId(event.getId().toString());
                message.getMessageProperties().setContentType("application/json");
                return message;
            }, correlation);

            if (correlation.getFuture() != null) {
                CorrelationData.Confirm confirm = correlation.getFuture().get(10, TimeUnit.SECONDS);
                if (!confirm.ack()) {
                    throw new IllegalStateException("RabbitMQ did not confirm event: " + confirm.reason());
                }
            }
            event.markPublished();
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Failed to publish outbox event id={}: {}", event.getId(), ex.getMessage());
            event.markFailed(ex.getMessage());
        }
        outboxEventRepository.save(event);
    }
}
