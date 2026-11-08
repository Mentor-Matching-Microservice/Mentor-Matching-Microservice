package com.mentormatching.payment.domain;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "booking_event_receipts")
public class BookingEventReceipt {
    @Id
    @Column(name = "event_id", length = 255)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();

    protected BookingEventReceipt() {}

    public BookingEventReceipt(String eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
    }

    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public Instant getReceivedAt() { return receivedAt; }
}
