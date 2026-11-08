package com.mentormatching.payment.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentEvent(String eventId, String eventType, Instant occurredAt, UUID paymentId,
                           UUID bookingId, UUID payerUserId, UUID mentorId, BigDecimal amount,
                           String currency, BigDecimal refundedAmount, String reason) {
}
