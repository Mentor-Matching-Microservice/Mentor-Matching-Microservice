package com.mentormatching.auth.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Một sự kiện chờ publish lên RabbitMQ. {@code id} chính là {@code eventId} trong payload.
 */
@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor
public class OutboxEvent {

    @Id
    @Column(nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 50, updatable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 100, updatable = false)
    private String eventType;

    @Column(name = "routing_key", nullable = false, length = 100, updatable = false)
    private String routingKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false, columnDefinition = "JSON")
    private String payload;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private Instant createdAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "published_at", columnDefinition = "DATETIME(6)")
    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 200)
    private String lastError;

    public OutboxEvent(UUID id, String aggregateType, UUID aggregateId, String eventType, String routingKey,
                       String payload, Instant createdAt) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.routingKey = routingKey;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public void markPublished(Instant at) {
        this.publishedAt = at;
        this.attempts++;
        this.lastError = null;
    }

    /** Ghi nhận lần gửi thất bại; chỉ lưu tên lớp lỗi (không lưu message có thể chứa dữ liệu nội bộ). */
    public void markFailed(Throwable error) {
        this.attempts++;
        String name = error.getClass().getSimpleName();
        this.lastError = name.length() > 200 ? name.substring(0, 200) : name;
    }
}
