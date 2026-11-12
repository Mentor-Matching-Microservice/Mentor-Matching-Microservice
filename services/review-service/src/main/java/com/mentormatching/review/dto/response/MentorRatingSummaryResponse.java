package com.mentormatching.review.dto.response;

import lombok.Builder;

import java.util.Map;

@Builder
public record MentorRatingSummaryResponse(
        Long mentorId,
        Double averageRating,
        Long totalReviews,
        Map<Integer, Long> ratingDistribution
) {
}
