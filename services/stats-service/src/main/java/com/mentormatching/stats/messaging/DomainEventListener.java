package com.mentormatching.stats.messaging;

import com.mentormatching.stats.event.DomainEvent;
import com.mentormatching.stats.event.InvalidEventException;
import com.mentormatching.stats.service.StatsIngestionService;
import com.mentormatching.stats.service.StatsIngestionService.Outcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Nhận sự kiện nghiệp vụ từ queue riêng của stats-service và cộng vào bộ đếm theo ngày.
 * Log chỉ ghi id/loại sự kiện, không ghi payload (có số tiền, id người dùng).
 */
@Component
@ConditionalOnProperty(name = "stats.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class DomainEventListener {

    private static final Logger log = LoggerFactory.getLogger(DomainEventListener.class);

    private final StatsIngestionService ingestion;
    private final ObjectMapper objectMapper;

    public DomainEventListener(StatsIngestionService ingestion, ObjectMapper objectMapper) {
        this.ingestion = ingestion;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "${stats.messaging.queue:stats.domain-events}",
            containerFactory = MessagingConfiguration.LISTENER_CONTAINER_FACTORY)
    public void onMessage(Message message) {
        DomainEvent event;
        try {
            event = DomainEvent.parse(message.getBody(), objectMapper);
        } catch (InvalidEventException ex) {
            log.warn("Rejecting malformed event messageId={} reason={}", message.getMessageProperties().getMessageId(),
                    ex.getMessage());
            throw ex;
        }
        Outcome outcome = ingestion.ingest(event);
        if (outcome == Outcome.IGNORED) {
            log.debug("Ignoring event type={} eventId={}", event.eventType(), event.eventId());
        } else {
            log.info("Event {} eventId={} type={}", outcome, event.eventId(), event.eventType());
        }
    }
}
