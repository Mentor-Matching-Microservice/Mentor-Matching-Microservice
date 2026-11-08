package com.mentormatching.booking.event;

import java.time.Instant;

public record BookingConfirmationFailedEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        Long bookingId,
        String reason
) {
}
