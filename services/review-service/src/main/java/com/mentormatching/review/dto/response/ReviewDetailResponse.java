package com.mentormatching.review.dto.response;

import com.mentormatching.review.entity.Review;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;

@Builder
public record ReviewDetailResponse(
        Long id,
        Long bookingId,
        String studentUserId,
        String studentName,
        Long mentorId,
        Integer rating,
        String comment,
        List<ReviewTagOptionResponse> tags,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ReviewDetailResponse from(Review review, List<ReviewTagOptionResponse> tags) {
        return ReviewDetailResponse.builder()
                .id(review.getId())
                .bookingId(review.getBookingId())
                .studentUserId(review.getStudentUserId())
                .studentName(review.getStudentName())
                .mentorId(review.getMentorId())
                .rating(review.getRating())
                .comment(review.getComment())
                .tags(tags)
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
