package com.mentormatching.notification.service;

import java.util.List;

import com.mentormatching.notification.config.WebSocketConfiguration;
import com.mentormatching.notification.dto.NotificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Đẩy thông báo tới {@code /user/queue/notifications} của người nhận.
 * Khi đang trong transaction, chỉ gửi SAU KHI commit để client không nhận thông báo đã bị rollback.
 * Lỗi gửi realtime chỉ ghi log: thông báo đã nằm trong hộp thư, client sẽ thấy khi tải lại danh sách.
 */
@Component
public class RealtimeNotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(RealtimeNotificationPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeNotificationPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishAfterCommit(List<NotificationResponse> notifications) {
        if (notifications.isEmpty()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notifications.forEach(RealtimeNotificationPublisher.this::send);
                }
            });
        } else {
            notifications.forEach(this::send);
        }
    }

    private void send(NotificationResponse notification) {
        try {
            messagingTemplate.convertAndSendToUser(notification.userId(), WebSocketConfiguration.NOTIFICATION_QUEUE,
                    notification);
        } catch (RuntimeException ex) {
            log.warn("Realtime delivery failed notificationId={} type={}", notification.id(), notification.type(), ex);
        }
    }
}
