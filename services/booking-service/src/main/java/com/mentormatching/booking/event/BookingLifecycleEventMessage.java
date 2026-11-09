package com.mentormatching.booking.event;

import java.time.Instant;

public record BookingLifecycleEventMessage(
        String eventId,
        String eventType,
        Instant occurredAt,
        String bookingId,
        String reason
) {
}
