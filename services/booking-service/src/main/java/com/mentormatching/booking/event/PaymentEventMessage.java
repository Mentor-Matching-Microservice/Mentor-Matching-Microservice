package com.mentormatching.booking.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentEventMessage(
        String eventId,
        String eventType,
        Instant occurredAt,
        String paymentId,
        String bookingId,
        String payerUserId,
        String mentorId,
        BigDecimal amount,
        String currency,
        BigDecimal refundedAmount,
        String reason
) {
}
