package com.mentormatching.notification.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mentormatching.notification.domain.Notification;
import com.mentormatching.notification.domain.NotificationType;

/**
 * Thông báo trả về cho REST và đẩy qua STOMP. Giữ tên field của dự án cũ ({@code isRead}, {@code userId})
 * để frontend dùng lại component hiện có; {@code id} là UUID dạng chuỗi.
 */
public record NotificationResponse(
        String id,
        String userId,
        NotificationType type,
        String title,
        String message,
        @JsonProperty("isRead") boolean isRead,
        String referenceType,
        String referenceId,
        Instant createdAt,
        Instant readAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getRecipientUserId(),
                notification.getType(), notification.getTitle(), notification.getMessage(), notification.isRead(),
                notification.getReferenceType(), notification.getReferenceId(), notification.getCreatedAt(),
                notification.getReadAt());
    }
}
