package com.mentormatching.payment.domain;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;
@Entity @Table(name="refund_transactions",uniqueConstraints=@UniqueConstraint(name="uk_refund_idempotency",columnNames="idempotency_key"))
public class RefundTransaction {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(name="payment_id",nullable=false) private UUID paymentId;
    @Column(name="idempotency_key",nullable=false,length=128,unique=true) private String idempotencyKey;
    @Column(name="provider_refund_id",nullable=false,length=255) private String providerRefundId;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal amount;
    @Column(nullable=false) private Instant createdAt=Instant.now();
    protected RefundTransaction(){}
    public RefundTransaction(UUID paymentId,String key,String providerRefundId,BigDecimal amount){this.paymentId=paymentId;this.idempotencyKey=key;this.providerRefundId=providerRefundId;this.amount=amount;}
    public UUID getPaymentId(){return paymentId;} public BigDecimal getAmount(){return amount;}
}
