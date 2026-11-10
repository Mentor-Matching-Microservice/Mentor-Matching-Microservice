package com.mentormatching.stats.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Sự kiện đã được cộng vào thống kê ({@code _id = eventId}), ghi cùng transaction với {@code $inc}
 * nên nhận lại cùng sự kiện không cộng hai lần. Tự xoá sau 90 ngày.
 */
@Document("processed_events")
public class ProcessedEvent {

    @Id
    private String eventId;
    private String eventType;
    @Indexed(name = "processed_ttl", expireAfter = "90d")
    private Instant processedAt;

    protected ProcessedEvent() {
        // dành cho Spring Data
    }

    public ProcessedEvent(String eventId, String eventType, Instant processedAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processedAt = processedAt;
    }

    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public Instant getProcessedAt() { return processedAt; }
}
