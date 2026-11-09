package com.mentormatching.review.event;

import java.time.Instant;

public record MentorRatingUpdatedEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        Long mentorId,
        Double averageRating,
        Long totalReviews,
        Long version
) {
}
