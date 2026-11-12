package com.mentormatching.payment.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mentormatching.payment.domain.Payment;
import com.mentormatching.payment.domain.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public final class PaymentModels {
    private PaymentModels(){}

    public record CreateRequest(@NotNull UUID bookingId){
        @JsonCreator
        public static CreateRequest from(@JsonProperty("bookingId") Object bookingId) {
            if (bookingId == null) {
                return new CreateRequest(null);
            }
            if (bookingId instanceof UUID uuid) {
                return new CreateRequest(uuid);
            }
            if (bookingId instanceof Number num) {
                return new CreateRequest(new UUID(0L, num.longValue()));
            }
            String str = bookingId.toString().trim();
            try {
                return new CreateRequest(UUID.fromString(str));
            } catch (IllegalArgumentException e) {
                try {
                    long num = Long.parseLong(str);
                    return new CreateRequest(new UUID(0L, num));
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Invalid bookingId format: " + str);
                }
            }
        }
    }

    public record RefundRequest(@Positive BigDecimal amount,String reason){}

    public record PaymentResponse(UUID id,UUID bookingId,UUID payerId,UUID mentorId,BigDecimal amount,String currency,
                                  PaymentStatus status,String checkoutUrl,String providerReferenceId,String providerTransactionId,
                                  BigDecimal refundedAmount,String failureReason,Instant expiresAt,Instant createdAt){
        public static PaymentResponse from(Payment p){return new PaymentResponse(p.getId(),p.getBookingId(),p.getPayerId(),p.getMentorId(),p.getAmount(),p.getCurrency(),p.getStatus(),p.getCheckoutUrl(),p.getProviderReferenceId(),p.getProviderTransactionId(),p.getRefundedAmount(),p.getFailureReason(),p.getExpiresAt(),p.getCreatedAt());}
    }

    public record BookingPaymentContext(UUID bookingId,UUID payerUserId,UUID mentorId,String bookingStatus,
                                        BigDecimal totalAmount,String currency,OffsetDateTime holdExpiresAt){}
}
