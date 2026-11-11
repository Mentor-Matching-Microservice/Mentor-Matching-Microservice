package com.mentormatching.mentor.dto.catalog.command;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record DeleteGradeCommand(
        @NotNull(message = "Grade ID is required")
        Long id
) {
}

