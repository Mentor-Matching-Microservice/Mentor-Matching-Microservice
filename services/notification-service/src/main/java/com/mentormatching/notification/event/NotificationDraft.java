package com.mentormatching.notification.event;

import java.util.UUID;

import com.mentormatching.notification.domain.NotificationType;

/** Nội dung một thông báo cần tạo cho một người nhận, do {@link EventNotificationMapper} sinh ra từ sự kiện. */
public record NotificationDraft(UUID recipientUserId, NotificationType type, String title, String message,
                                String referenceType, String referenceId) {
}
