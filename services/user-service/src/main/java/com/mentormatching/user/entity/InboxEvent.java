package com.mentormatching.user.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Sự kiện từ service khác đã được xử lý; {@code event_id} là khoá chính nên mỗi sự kiện chỉ áp dụng một lần.
 */
@Entity
@Table(name = "inbox_events")
@Getter
@NoArgsConstructor
public class InboxEvent {

    @Id
    @Column(name = "event_id", nullable = false, length = 100, updatable = false)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100, updatable = false)
    private String eventType;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "processed_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private Instant processedAt;

    public InboxEvent(String eventId, String eventType, Instant processedAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processedAt = processedAt;
    }
}
