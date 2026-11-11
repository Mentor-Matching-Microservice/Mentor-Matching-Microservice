package com.mentormatching.mentor.dto.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;

@Builder
public record MentorRatingSyncRequest(
        @NotNull @PositiveOrZero Double averageRating,
        @NotNull @PositiveOrZero Long totalReviews,
        @NotNull @PositiveOrZero Long version
) {
}
