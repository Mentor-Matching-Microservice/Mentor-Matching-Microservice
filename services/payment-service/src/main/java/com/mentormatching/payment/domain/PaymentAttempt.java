package com.mentormatching.payment.domain;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment_attempts")
public class PaymentAttempt {
    public enum Status { PENDING, COMPLETED, FAILED, EXPIRED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Status status;

    @Column(name = "provider_reference_id", length = 255)
    private String providerReferenceId;

    @Column(name = "checkout_url", length = 2048)
    private String checkoutUrl;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected PaymentAttempt() {}

    public PaymentAttempt(UUID paymentId, int attemptNumber, String providerReferenceId,
                          String checkoutUrl, Instant expiresAt) {
        this.paymentId = paymentId;
        this.attemptNumber = attemptNumber;
        this.providerReferenceId = providerReferenceId;
        this.checkoutUrl = checkoutUrl;
        this.expiresAt = expiresAt;
        this.status = Status.PENDING;
    }

    public void completed() { this.status = Status.COMPLETED; }
    public void failed(String reason) { this.status = Status.FAILED; this.failureReason = reason; }
    public void expired() { this.status = Status.EXPIRED; }

    public UUID getId() { return id; }
    public UUID getPaymentId() { return paymentId; }
    public int getAttemptNumber() { return attemptNumber; }
    public Status getStatus() { return status; }
    public String getProviderReferenceId() { return providerReferenceId; }
    public String getCheckoutUrl() { return checkoutUrl; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
}
