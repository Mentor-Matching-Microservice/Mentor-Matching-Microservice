package com.mentormatching.auth.messaging;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.mentormatching.auth.entity.OutboxEvent;
import com.mentormatching.auth.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Định kỳ gửi các sự kiện outbox chưa publish lên {@code mentor.events}, theo thứ tự tạo.
 * Mỗi sự kiện chỉ được đánh dấu đã gửi khi broker xác nhận (publisher confirm); nếu broker không phản hồi hoặc
 * từ chối thì giữ nguyên để lần chạy sau gửi bù (at-least-once; consumer chống trùng theo {@code eventId}).
 * Gặp lỗi thì dừng lô hiện tại để không gửi sự kiện sau trước sự kiện trước.
 */
@Component
@ConditionalOnProperty(name = "auth.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outbox;
    private final RabbitTemplate rabbit;
    private final AuthMessagingProperties properties;
    private final TransactionTemplate transaction;

    public OutboxPublisher(OutboxEventRepository outbox, RabbitTemplate rabbit, AuthMessagingProperties properties,
                           PlatformTransactionManager transactionManager) {
        this.outbox = outbox;
        this.rabbit = rabbit;
        this.properties = properties;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Scheduled(fixedDelayString = "${auth.messaging.publish-delay:2s}", initialDelayString = "${auth.messaging.publish-initial-delay:2s}")
    public void publishPending() {
        Integer sent = transaction.execute(status -> publishBatch());
        if (sent != null && sent > 0) {
            log.debug("Published {} outbox events", sent);
        }
    }

    /** @return số sự kiện đã gửi thành công trong lô */
    int publishBatch() {
        List<OutboxEvent> batch = outbox.lockNextBatch();
        int sent = 0;
        for (OutboxEvent event : batch) {
            try {
                send(event);
                event.markPublished(Instant.now());
                outbox.save(event);
                sent++;
            } catch (Exception ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                event.markFailed(ex);
                outbox.save(event);
                log.warn("Outbox publish failed eventId={} type={} attempts={} error={}", event.getId(),
                        event.getEventType(), event.getAttempts(), ex.getClass().getSimpleName());
                break; // giữ thứ tự: không gửi sự kiện sau khi sự kiện trước chưa đi được
            }
        }
        return sent;
    }

    private void send(OutboxEvent event) throws Exception {
        CorrelationData correlation = new CorrelationData(event.getId().toString());
        rabbit.convertAndSend(properties.exchange(), event.getRoutingKey(), event.getPayload(), message -> {
            message.getMessageProperties().setMessageId(event.getId().toString());
            message.getMessageProperties().setContentType("application/json");
            message.getMessageProperties().setType(event.getEventType());
            return message;
        }, correlation);
        CorrelationData.Confirm confirm = correlation.getFuture()
                .get(properties.confirmTimeout().toMillis(), TimeUnit.MILLISECONDS);
        if (!confirm.ack()) {
            throw new IllegalStateException("Broker rejected the event");
        }
    }
}
