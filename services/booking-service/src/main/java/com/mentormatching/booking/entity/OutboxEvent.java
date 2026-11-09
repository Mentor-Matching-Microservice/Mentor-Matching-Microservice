package com.mentormatching.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "aggregate_id", nullable = false, length = 120)
    private String aggregateId;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "publish_attempts", nullable = false)
    private int publishAttempts;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    public static OutboxEvent of(String eventType, String aggregateId, String payload) {
        return OutboxEvent.builder()
                .id(UUID.randomUUID())
                .eventType(eventType)
                .aggregateId(aggregateId)
                .payload(payload)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .publishAttempts(0)
                .build();
    }

    public void markPublished() {
        this.publishedAt = OffsetDateTime.now(ZoneOffset.UTC);
        this.lastError = null;
    }

    public void markFailed(String error) {
        this.publishAttempts++;
        this.lastError = error != null && error.length() > 2000 ? error.substring(0, 2000) : error;
    }
}
