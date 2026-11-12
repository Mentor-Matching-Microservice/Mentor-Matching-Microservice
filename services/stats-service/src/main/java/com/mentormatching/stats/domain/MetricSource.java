package com.mentormatching.stats.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Loại sự kiện đã từng nhận ({@code _id = eventType}). Chỉ số có nguồn chưa được publish trên {@code develop}
 * (ví dụ {@code AccountRegistered}) được báo là chưa có dữ liệu ({@code partialData}) cho tới khi có bản ghi ở đây.
 */
@Document("metric_sources")
public class MetricSource {

    @Id
    private String eventType;
    private Instant firstSeenAt;

    protected MetricSource() {
        // dành cho Spring Data
    }

    public String getEventType() { return eventType; }
    public Instant getFirstSeenAt() { return firstSeenAt; }
}
