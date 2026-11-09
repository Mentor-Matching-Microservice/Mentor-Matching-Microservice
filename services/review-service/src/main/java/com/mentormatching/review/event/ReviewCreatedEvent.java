package com.mentormatching.review.event;

import java.time.Instant;

public record ReviewCreatedEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        Long reviewId,
        Long bookingId,
        String studentUserId,
        Long mentorId,
        Integer rating
) {
}
