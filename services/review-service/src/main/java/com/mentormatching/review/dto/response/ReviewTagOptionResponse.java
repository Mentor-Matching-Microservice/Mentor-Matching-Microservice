package com.mentormatching.review.dto.response;

import com.mentormatching.review.entity.ReviewTagOption;
import lombok.Builder;

@Builder
public record ReviewTagOptionResponse(
        Long id,
        String name,
        String description
) {
    public static ReviewTagOptionResponse from(ReviewTagOption option) {
        return ReviewTagOptionResponse.builder()
                .id(option.getId())
                .name(option.getName())
                .description(option.getDescription())
                .build();
    }
}
