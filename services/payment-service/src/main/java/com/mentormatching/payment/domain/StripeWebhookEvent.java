package com.mentormatching.payment.domain;
import java.time.Instant;
import jakarta.persistence.*;
@Entity @Table(name="stripe_webhook_events")
public class StripeWebhookEvent {
    @Id @Column(name="event_id",length=255) private String eventId;
    @Column(nullable=false,length=120) private String eventType;
    @Column(nullable=false) private Instant receivedAt=Instant.now();
    protected StripeWebhookEvent(){}
    public StripeWebhookEvent(String id,String type){eventId=id;eventType=type;}
}
