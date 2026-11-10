package com.mentormatching.notification.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Một thông báo trong hộp thư của đúng một người nhận ({@code recipientUserId} = accountId/sub của JWT).
 *
 * <p>Index:
 * <ul>
 *   <li>{@code recipient_created}: liệt kê hộp thư theo thời gian mới nhất.</li>
 *   <li>{@code recipient_unread}: đếm/đánh dấu chưa đọc.</li>
 *   <li>{@code event_recipient_unique}: một sự kiện chỉ tạo tối đa một thông báo cho mỗi người nhận
 *       (lớp chống trùng thứ hai, sau bảng inbox); chỉ áp dụng cho thông báo sinh từ sự kiện.</li>
 * </ul>
 */
@Document("notifications")
@CompoundIndexes({
        @CompoundIndex(name = "recipient_created", def = "{'recipientUserId': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "recipient_unread", def = "{'recipientUserId': 1, 'read': 1}"),
        @CompoundIndex(name = "event_recipient_unique", def = "{'sourceEventId': 1, 'recipientUserId': 1}",
                unique = true, partialFilter = "{'sourceEventId': {$exists: true}}")
})
public class Notification {

    public static final int TITLE_MAX_LENGTH = 200;
    public static final int MESSAGE_MAX_LENGTH = 1000;

    @Id
    private String id;
    private String recipientUserId;
    private NotificationType type;
    private String title;
    private String message;
    private String referenceType;
    private String referenceId;
    private String sourceEventId;
    private boolean read;
    private Instant createdAt;
    private Instant readAt;

    protected Notification() {
        // dành cho Spring Data
    }

    private Notification(UUID recipientUserId, NotificationType type, String title, String message,
                         String referenceType, String referenceId, String sourceEventId, Instant createdAt) {
        this.id = UUID.randomUUID().toString();
        this.recipientUserId = Objects.requireNonNull(recipientUserId, "recipientUserId").toString();
        this.type = Objects.requireNonNull(type, "type");
        this.title = truncate(requireText(title, "title"), TITLE_MAX_LENGTH);
        this.message = truncate(requireText(message, "message"), MESSAGE_MAX_LENGTH);
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.sourceEventId = sourceEventId;
        this.read = false;
        this.createdAt = toStoredPrecision(Objects.requireNonNull(createdAt, "createdAt"));
    }

    /** Tạo thông báo mới, chưa đọc. {@code sourceEventId} null nếu không sinh từ sự kiện. */
    public static Notification create(UUID recipientUserId, NotificationType type, String title, String message,
                                      String referenceType, String referenceId, String sourceEventId,
                                      Instant createdAt) {
        return new Notification(recipientUserId, type, title, message, referenceType, referenceId,
                sourceEventId, createdAt);
    }

    /** Đánh dấu đã đọc; gọi lại nhiều lần không đổi thời điểm đọc đầu tiên. */
    public void markRead(Instant at) {
        if (!read) {
            read = true;
            readAt = toStoredPrecision(at);
        }
    }

    public boolean isOwnedBy(UUID userId) {
        return userId != null && recipientUserId.equals(userId.toString());
    }

    public String getId() { return id; }
    public String getRecipientUserId() { return recipientUserId; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getReferenceType() { return referenceType; }
    public String getReferenceId() { return referenceId; }
    public String getSourceEventId() { return sourceEventId; }
    public boolean isRead() { return read; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }

    /** MongoDB lưu thời gian tới mili giây; cắt trước để giá trị trả về trùng với giá trị đọc lại. */
    public static Instant toStoredPrecision(Instant instant) {
        return instant.truncatedTo(ChronoUnit.MILLIS);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.strip();
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }
}
