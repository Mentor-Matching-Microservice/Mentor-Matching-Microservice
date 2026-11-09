package com.mentormatching.review.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingReviewEligibilityResponse(
        Long bookingId,
        String studentUserId,
        Long mentorId,
        String mentorUserId,
        String status,
        boolean eligibleForReview
) {
}
