package com.mentormatching.booking.event;

import java.math.BigDecimal;
import java.time.Instant;

public record BookingConfirmedEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        Long bookingId,
        String studentUserId,
        Long mentorId,
        String mentorUserId,
        BigDecimal totalAmount,
        String currency
) {
}
