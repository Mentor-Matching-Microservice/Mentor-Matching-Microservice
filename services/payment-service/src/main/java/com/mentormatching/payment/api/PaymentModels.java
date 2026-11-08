package com.mentormatching.payment.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import com.mentormatching.payment.domain.Payment;
import com.mentormatching.payment.domain.PaymentStatus;
import jakarta.validation.constraints.NotNull;

public final class PaymentModels {
    private PaymentModels(){}
    public record CreateRequest(@NotNull UUID bookingId){}
    public record PaymentResponse(UUID id,UUID bookingId,UUID payerId,UUID mentorId,BigDecimal amount,String currency,
                                  PaymentStatus status,String checkoutUrl,String providerReferenceId,String providerTransactionId,
                                  String failureReason,Instant expiresAt,Instant createdAt){
        public static PaymentResponse from(Payment p){return new PaymentResponse(p.getId(),p.getBookingId(),p.getPayerId(),p.getMentorId(),p.getAmount(),p.getCurrency(),p.getStatus(),p.getCheckoutUrl(),p.getProviderReferenceId(),p.getProviderTransactionId(),p.getFailureReason(),p.getExpiresAt(),p.getCreatedAt());}
    }
    public record BookingPaymentContext(UUID bookingId,UUID payerUserId,UUID mentorId,String bookingStatus,
                                        BigDecimal totalAmount,String currency,OffsetDateTime holdExpiresAt){}
}
