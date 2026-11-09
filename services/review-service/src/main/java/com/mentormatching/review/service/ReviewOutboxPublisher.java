package com.mentormatching.review.service;

import com.mentormatching.review.entity.OutboxEvent;
import com.mentormatching.review.repository.OutboxEventRepository;
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
@ConditionalOnProperty(name = "review.messaging.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class ReviewOutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String ratingRoutingKey;

    public ReviewOutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            RabbitTemplate rabbitTemplate,
            @Value("${review.messaging.exchange:mentor.events}") String exchange,
            @Value("${review.messaging.mentor-rating-routing-key:mentor.rating}") String ratingRoutingKey) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.ratingRoutingKey = ratingRoutingKey;
    }

    @Scheduled(fixedDelayString = "${REVIEW_OUTBOX_PUBLISH_DELAY_MS:5000}")
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
        for (OutboxEvent event : pending) {
            publish(event);
        }
    }

    private void publish(OutboxEvent event) {
        try {
            String routingKey = "MentorRatingUpdated".equals(event.getEventType())
                    ? ratingRoutingKey
                    : "review.events";

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
