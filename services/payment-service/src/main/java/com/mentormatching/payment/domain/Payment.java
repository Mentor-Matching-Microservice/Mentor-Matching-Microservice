package com.mentormatching.payment.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;

@Entity @Table(name="payments",uniqueConstraints={@UniqueConstraint(name="uk_payment_booking",columnNames="booking_id"),@UniqueConstraint(name="uk_payment_payer_idempotency",columnNames={"payer_id","idempotency_key"}),@UniqueConstraint(name="uk_payment_stripe_session",columnNames="provider_reference_id")})
public class Payment {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(name="booking_id",nullable=false,unique=true) private UUID bookingId;
    @Column(name="payer_id",nullable=false) private UUID payerId;
    @Column(name="mentor_id",nullable=false) private UUID mentorId;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal amount;
    @Column(nullable=false,length=3) private String currency;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=24) private PaymentStatus status;
    @Column(name="idempotency_key",nullable=false,length=128) private String idempotencyKey;
    @Column(name="provider_reference_id",length=255) private String providerReferenceId;
    @Column(name="provider_transaction_id",length=255) private String providerTransactionId;
    @Column(name="checkout_url",length=2048) private String checkoutUrl;
    @Column(name="expires_at") private Instant expiresAt;
    @Column(name="checkout_attempt",nullable=false) private int checkoutAttempt=0;
    @Column(name="failure_reason",length=1000) private String failureReason;
    @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now();
    @Column(name="updated_at",nullable=false) private Instant updatedAt=Instant.now();
    protected Payment(){}
    public Payment(UUID bookingId,UUID payerId,UUID mentorId,BigDecimal amount,String currency,String idempotencyKey){
        this.bookingId=bookingId;this.payerId=payerId;this.mentorId=mentorId;this.amount=amount;this.currency=currency.toLowerCase();this.idempotencyKey=idempotencyKey;this.status=PaymentStatus.PENDING;
    }
    public void checkout(String sessionId,String url,Instant expires){checkoutAttempt++;providerReferenceId=sessionId;checkoutUrl=url;expiresAt=expires;updatedAt=Instant.now();}
    public void paid(String intentId){status=PaymentStatus.PAID;providerTransactionId=intentId;failureReason=null;updatedAt=Instant.now();}
    public void failed(String reason){if(status==PaymentStatus.PAID)return;status=PaymentStatus.FAILED;failureReason=reason;updatedAt=Instant.now();}
    public UUID getId(){return id;} public UUID getBookingId(){return bookingId;} public UUID getPayerId(){return payerId;} public UUID getMentorId(){return mentorId;}
    public BigDecimal getAmount(){return amount;} public String getCurrency(){return currency;} public PaymentStatus getStatus(){return status;}
    public String getIdempotencyKey(){return idempotencyKey;} public String getProviderReferenceId(){return providerReferenceId;} public String getProviderTransactionId(){return providerTransactionId;}
    public String getCheckoutUrl(){return checkoutUrl;} public Instant getExpiresAt(){return expiresAt;}
    public String getFailureReason(){return failureReason;} public Instant getCreatedAt(){return createdAt;}
    public int getCheckoutAttempt(){return checkoutAttempt;}
}
