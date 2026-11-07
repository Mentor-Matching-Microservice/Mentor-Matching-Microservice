package com.mentormatching.booking.domain;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import jakarta.persistence.*;
@Entity @Table(name="bookings")
public class Booking {
    @Id private UUID id;
    @Column(name="learner_id",nullable=false) private UUID learnerId;
    @Column(name="mentor_id",nullable=false) private UUID mentorId;
    @Enumerated(EnumType.STRING) @Column(name="booking_status",nullable=false) private BookingStatus status;
    @Column(name="total_amount",nullable=false,precision=12,scale=2) private BigDecimal totalAmount;
    @Column(nullable=false,length=3) private String currency;
    @Column(name="start_at",nullable=false) private OffsetDateTime startAt;
    @Column(name="payment_hold_expires_at",nullable=false) private OffsetDateTime paymentHoldExpiresAt;
    protected Booking(){}
    public UUID getId(){return id;} public UUID getLearnerId(){return learnerId;} public UUID getMentorId(){return mentorId;}
    public BookingStatus getStatus(){return status;} public BigDecimal getTotalAmount(){return totalAmount;} public String getCurrency(){return currency;}
    public OffsetDateTime getStartAt(){return startAt;} public OffsetDateTime getPaymentHoldExpiresAt(){return paymentHoldExpiresAt;}
}
