package com.mentormatching.notification.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Bản ghi inbox: sự kiện có {@code eventId} này đã được xử lý xong (thông báo đã lưu).
 * {@code _id = eventId} nên MongoDB tự bảo đảm mỗi sự kiện chỉ được ghi nhận một lần.
 * Tự xoá sau 30 ngày (TTL index) — đủ dài so với thời gian giữ lại/replay của outbox phía publisher.
 */
@Document("inbox_events")
public class InboxEvent {

    @Id
    private String eventId;
    private String eventType;
    private int notificationsCreated;
    @Indexed(name = "received_ttl", expireAfter = "30d")
    private Instant receivedAt;

    protected InboxEvent() {
        // dành cho Spring Data
    }

    public InboxEvent(String eventId, String eventType, int notificationsCreated, Instant receivedAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.notificationsCreated = notificationsCreated;
        this.receivedAt = receivedAt;
    }

    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public int getNotificationsCreated() { return notificationsCreated; }
    public Instant getReceivedAt() { return receivedAt; }
}
