package com.mentormatching.notification.messaging;

import com.mentormatching.notification.event.DomainEvent;
import com.mentormatching.notification.event.InvalidEventException;
import com.mentormatching.notification.service.NotificationIngestionService;
import com.mentormatching.notification.service.NotificationIngestionService.Outcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Nhận sự kiện nghiệp vụ từ queue riêng của notification-service. Đọc thẳng byte JSON của message nên
 * không phụ thuộc message converter của publisher. Log chỉ ghi id/loại sự kiện, không ghi payload.
 */
@Component
@ConditionalOnProperty(name = "notification.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class DomainEventListener {

    private static final Logger log = LoggerFactory.getLogger(DomainEventListener.class);

    private final NotificationIngestionService ingestion;
    private final ObjectMapper objectMapper;

    public DomainEventListener(NotificationIngestionService ingestion, ObjectMapper objectMapper) {
        this.ingestion = ingestion;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "${notification.messaging.queue:notification.domain-events}",
            containerFactory = MessagingConfiguration.LISTENER_CONTAINER_FACTORY)
    public void onMessage(Message message) {
        String messageId = message.getMessageProperties().getMessageId();
        DomainEvent event;
        try {
            event = DomainEvent.parse(message.getBody(), objectMapper);
        } catch (InvalidEventException ex) {
            log.warn("Rejecting malformed event messageId={} reason={}", messageId, ex.getMessage());
            throw ex;
        }
        try {
            Outcome outcome = ingestion.ingest(event);
            if (outcome == Outcome.IGNORED) {
                log.debug("Ignoring unsupported event type={} eventId={}", event.eventType(), event.eventId());
            } else {
                log.info("Event {} eventId={} type={}", outcome, event.eventId(), event.eventType());
            }
        } catch (InvalidEventException ex) {
            log.warn("Rejecting invalid event eventId={} type={} reason={}", event.eventId(), event.eventType(),
                    ex.getMessage());
            throw ex;
        }
    }
}
