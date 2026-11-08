package com.mentormatching.payment.domain;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "webhook_receipts")
public class WebhookReceipt {
    @Id
    @Column(name = "provider_event_id", length = 255)
    private String providerEventId;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();

    protected WebhookReceipt() {}

    public WebhookReceipt(String providerEventId, String eventType) {
        this.providerEventId = providerEventId;
        this.eventType = eventType;
    }

    public String getProviderEventId() { return providerEventId; }
    public String getEventType() { return eventType; }
    public Instant getReceivedAt() { return receivedAt; }
}
