package com.mentormatching.payment.domain;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(nullable = false, columnDefinition = "nvarchar(max)")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "publish_attempts", nullable = false)
    private int publishAttempts;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    protected OutboxEvent() {}

    public OutboxEvent(UUID id, String eventType, UUID aggregateId, String payload) {
        this.id = id;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.payload = payload;
    }

    public void published() {
        publishedAt = Instant.now();
        lastError = null;
    }

    public void failed(String error) {
        publishAttempts++;
        lastError = error == null ? "Unknown broker error" : error.substring(0, Math.min(error.length(), 2000));
    }

    public UUID getId() { return id; }
    public String getEventType() { return eventType; }
    public UUID getAggregateId() { return aggregateId; }
    public String getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public int getPublishAttempts() { return publishAttempts; }
    public String getLastError() { return lastError; }
}
