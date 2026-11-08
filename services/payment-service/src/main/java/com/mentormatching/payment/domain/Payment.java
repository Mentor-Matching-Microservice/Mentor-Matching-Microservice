package com.mentormatching.payment.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "payments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_booking", columnNames = "booking_id"),
        @UniqueConstraint(name = "uk_payment_payer_idempotency", columnNames = {"payer_id", "idempotency_key"})
})
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "booking_id", nullable = false, unique = true)
    private UUID bookingId;

    @Column(name = "payer_id", nullable = false)
    private UUID payerId;

    @Column(name = "mentor_id", nullable = false)
    private UUID mentorId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PaymentStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "provider_reference_id", length = 255)
    private String providerReferenceId;

    @Column(name = "provider_transaction_id", length = 255)
    private String providerTransactionId;

    @Column(name = "checkout_url", length = 2048)
    private String checkoutUrl;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "checkout_attempt", nullable = false)
    private int checkoutAttempt;

    @Column(name = "refunded_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Payment() {}

    public Payment(UUID bookingId, UUID payerId, UUID mentorId, BigDecimal amount, String currency,
                   String idempotencyKey) {
        this.bookingId = bookingId;
        this.payerId = payerId;
        this.mentorId = mentorId;
        this.amount = amount;
        this.currency = currency.toLowerCase(Locale.ROOT);
        this.idempotencyKey = idempotencyKey;
        this.status = PaymentStatus.PENDING;
        this.refundedAmount = BigDecimal.ZERO;
    }

    public void checkout(String sessionId, String url, Instant expires) {
        checkoutAttempt++;
        providerReferenceId = sessionId;
        checkoutUrl = url;
        expiresAt = expires;
        updatedAt = Instant.now();
    }

    public boolean paid(String intentId) {
        if (status == PaymentStatus.PAID || status == PaymentStatus.REFUNDED
                || status == PaymentStatus.PARTIALLY_REFUNDED) {
            return false;
        }
        status = PaymentStatus.PAID;
        providerTransactionId = intentId;
        failureReason = null;
        updatedAt = Instant.now();
        return true;
    }

    public boolean failed(String reason) {
        if (status == PaymentStatus.PAID || status == PaymentStatus.PARTIALLY_REFUNDED
                || status == PaymentStatus.REFUNDED || status == PaymentStatus.FAILED) {
            return false;
        }
        status = PaymentStatus.FAILED;
        failureReason = reason;
        updatedAt = Instant.now();
        return true;
    }

    public void refund(BigDecimal amount) {
        refundedAmount = refundedAmount.add(amount);
        status = refundedAmount.compareTo(this.amount) >= 0
                ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
        updatedAt = Instant.now();
    }

    public boolean reconcileRefund(BigDecimal total) {
        BigDecimal reconciled = total.min(amount).max(BigDecimal.ZERO);
        if (refundedAmount.compareTo(reconciled) == 0) {
            return false;
        }
        refundedAmount = reconciled;
        status = refundedAmount.compareTo(amount) >= 0
                ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
        updatedAt = Instant.now();
        return true;
    }

    public UUID getId() { return id; }
    public UUID getBookingId() { return bookingId; }
    public UUID getPayerId() { return payerId; }
    public UUID getMentorId() { return mentorId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getProviderReferenceId() { return providerReferenceId; }
    public String getProviderTransactionId() { return providerTransactionId; }
    public String getCheckoutUrl() { return checkoutUrl; }
    public Instant getExpiresAt() { return expiresAt; }
    public int getCheckoutAttempt() { return checkoutAttempt; }
    public BigDecimal getRefundedAmount() { return refundedAmount; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
}
