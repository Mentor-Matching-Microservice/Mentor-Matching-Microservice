package com.mentormatching.notification.domain;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Một lần đẩy thông báo qua kênh realtime. Lưu cả lần thành công và thất bại để tra cứu
 * "thông báo này đã được đẩy chưa, thử mấy lần, lỗi gì". Tự xoá sau 30 ngày.
 * Chỉ lưu tên lớp lỗi, không lưu message lỗi (có thể chứa dữ liệu nội bộ).
 */
@Document("delivery_attempts")
@CompoundIndex(name = "notification_attempt", def = "{'notificationId': 1, 'attempt': 1}")
public class DeliveryAttempt {

    public enum Channel { WEBSOCKET }

    public enum Outcome { SENT, FAILED }

    @Id
    private String id;
    private String notificationId;
    private String recipientUserId;
    private Channel channel;
    private int attempt;
    private Outcome outcome;
    private String error;
    @Indexed(name = "attempted_ttl", expireAfter = "30d")
    private Instant attemptedAt;

    protected DeliveryAttempt() {
        // dành cho Spring Data
    }

    private DeliveryAttempt(String notificationId, String recipientUserId, Channel channel, int attempt,
                            Outcome outcome, String error, Instant attemptedAt) {
        this.id = UUID.randomUUID().toString();
        this.notificationId = notificationId;
        this.recipientUserId = recipientUserId;
        this.channel = channel;
        this.attempt = attempt;
        this.outcome = outcome;
        this.error = error;
        this.attemptedAt = Notification.toStoredPrecision(attemptedAt);
    }

    public static DeliveryAttempt sent(String notificationId, String recipientUserId, int attempt, Instant at) {
        return new DeliveryAttempt(notificationId, recipientUserId, Channel.WEBSOCKET, attempt, Outcome.SENT, null, at);
    }

    public static DeliveryAttempt failed(String notificationId, String recipientUserId, int attempt, Throwable error,
                                         Instant at) {
        return new DeliveryAttempt(notificationId, recipientUserId, Channel.WEBSOCKET, attempt, Outcome.FAILED,
                error.getClass().getSimpleName(), at);
    }

    public String getId() { return id; }
    public String getNotificationId() { return notificationId; }
    public String getRecipientUserId() { return recipientUserId; }
    public Channel getChannel() { return channel; }
    public int getAttempt() { return attempt; }
    public Outcome getOutcome() { return outcome; }
    public String getError() { return error; }
    public Instant getAttemptedAt() { return attemptedAt; }
}
