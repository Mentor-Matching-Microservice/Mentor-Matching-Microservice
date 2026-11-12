package com.mentormatching.mentor.messaging;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.mentormatching.mentor.entity.OutboxEvent;
import com.mentormatching.mentor.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Component
@ConditionalOnProperty(name = "mentor.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class MentorOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(MentorOutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final MentorMessagingProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    public MentorOutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            RabbitTemplate rabbitTemplate,
            MentorMessagingProperties properties,
            PlatformTransactionManager transactionManager,
            ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${mentor.messaging.publish-delay:2s}", initialDelayString = "${mentor.messaging.publish-initial-delay:2s}")
    public void publishPending() {
        Integer sent = transactionTemplate.execute(status -> publishBatch());
        if (sent != null && sent > 0) {
            log.debug("Published {} mentor outbox events", sent);
        }
    }

    public int publishBatch() {
        List<OutboxEvent> pending = outboxEventRepository.findPendingEvents(PageRequest.of(0, 50));
        int sent = 0;
        for (OutboxEvent event : pending) {
            try {
                send(event);
                event.setPublishedAt(Instant.now());
                outboxEventRepository.save(event);
                sent++;
            } catch (Exception ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                event.setPublishAttempts(event.getPublishAttempts() + 1);
                event.setLastError(ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
                outboxEventRepository.save(event);
                log.warn("Failed to publish outbox event id={} type={}: {}", event.getId(), event.getEventType(), ex.getMessage());
                break;
            }
        }
        return sent;
    }

    private void send(OutboxEvent event) throws Exception {
        String routingKey = "MentorApproved".equalsIgnoreCase(event.getEventType())
                ? properties.approvedRoutingKey()
                : "MentorRejected".equalsIgnoreCase(event.getEventType())
                        ? properties.rejectedRoutingKey()
                        : "mentor." + event.getEventType().toLowerCase();

        String jsonPayload = preparePayload(event);

        CorrelationData correlation = new CorrelationData(event.getId());
        rabbitTemplate.convertAndSend(properties.exchange(), routingKey, jsonPayload, message -> {
            message.getMessageProperties().setMessageId(event.getId());
            message.getMessageProperties().setContentType("application/json");
            message.getMessageProperties().setType(event.getEventType());
            return message;
        }, correlation);

        CorrelationData.Confirm confirm = correlation.getFuture()
                .get(properties.confirmTimeout().toMillis(), TimeUnit.MILLISECONDS);
        if (!confirm.ack()) {
            throw new IllegalStateException("Broker rejected event: " + confirm.reason());
        }
    }

    private String preparePayload(OutboxEvent event) {
        try {
            JsonNode node = objectMapper.readTree(event.getPayload());
            if (node instanceof ObjectNode obj) {
                if (!obj.has("eventId")) {
                    obj.put("eventId", event.getId());
                }
                if (!obj.has("eventType")) {
                    obj.put("eventType", event.getEventType());
                }
                return objectMapper.writeValueAsString(obj);
            }
        } catch (Exception e) {
            log.warn("Could not enrich outbox payload with eventId/eventType, falling back to raw payload: {}", e.getMessage());
        }
        return event.getPayload();
    }
}
