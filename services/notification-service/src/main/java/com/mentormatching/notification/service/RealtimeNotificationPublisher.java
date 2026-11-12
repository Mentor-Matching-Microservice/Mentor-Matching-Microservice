package com.mentormatching.notification.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.mentormatching.notification.config.WebSocketConfiguration;
import com.mentormatching.notification.domain.DeliveryAttempt;
import com.mentormatching.notification.dto.NotificationResponse;
import com.mentormatching.notification.repository.DeliveryAttemptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Đẩy thông báo tới {@code /user/queue/notifications} của người nhận.
 *
 * <ul>
 *   <li>Trong transaction: chỉ gửi SAU KHI commit, để client không nhận thông báo đã bị rollback.</li>
 *   <li>Lỗi tạm thời khi đẩy: thử lại tối đa {@code notification.delivery.max-attempts} lần với backoff nhân đôi (#81).</li>
 *   <li>Mỗi lần thử được ghi vào {@code delivery_attempts}. Hết số lần thử vẫn không làm mất thông báo:
 *       nó đã nằm trong hộp thư, client sẽ thấy khi tải lại danh sách.</li>
 * </ul>
 */
@Component
@EnableConfigurationProperties(DeliveryProperties.class)
public class RealtimeNotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(RealtimeNotificationPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final DeliveryAttemptRepository attempts;
    private final DeliveryProperties properties;
    private final Clock clock;

    public RealtimeNotificationPublisher(SimpMessagingTemplate messagingTemplate, DeliveryAttemptRepository attempts,
                                         DeliveryProperties properties, Clock clock) {
        this.messagingTemplate = messagingTemplate;
        this.attempts = attempts;
        this.properties = properties;
        this.clock = clock;
    }

    public void publishAfterCommit(List<NotificationResponse> notifications) {
        if (notifications.isEmpty()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notifications.forEach(RealtimeNotificationPublisher.this::deliver);
                }
            });
        } else {
            notifications.forEach(this::deliver);
        }
    }

    /** @return true nếu đã giao cho broker STOMP ở một trong các lần thử */
    boolean deliver(NotificationResponse notification) {
        long backoff = properties.initialBackoff().toMillis();
        for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
            try {
                messagingTemplate.convertAndSendToUser(notification.userId(), WebSocketConfiguration.NOTIFICATION_QUEUE,
                        notification);
                record(DeliveryAttempt.sent(notification.id(), notification.userId(), attempt, Instant.now(clock)));
                return true;
            } catch (RuntimeException ex) {
                record(DeliveryAttempt.failed(notification.id(), notification.userId(), attempt, ex, Instant.now(clock)));
                if (attempt == properties.maxAttempts()) {
                    log.warn("Realtime delivery gave up notificationId={} type={} attempts={} error={}",
                            notification.id(), notification.type(), attempt, ex.getClass().getSimpleName());
                    return false;
                }
                if (!sleep(backoff)) {
                    return false;
                }
                backoff *= 2;
            }
        }
        return false;
    }

    private void record(DeliveryAttempt attempt) {
        try {
            attempts.insert(attempt);
        } catch (RuntimeException ex) {
            // Ghi lịch sử thất bại không được làm hỏng việc đẩy thông báo.
            log.warn("Could not record delivery attempt notificationId={} error={}", attempt.getNotificationId(),
                    ex.getClass().getSimpleName());
        }
    }

    private static boolean sleep(long millis) {
        if (millis <= 0) {
            return true;
        }
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
