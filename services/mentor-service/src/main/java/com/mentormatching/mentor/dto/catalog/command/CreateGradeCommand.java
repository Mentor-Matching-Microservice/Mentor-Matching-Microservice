package com.mentormatching.mentor.dto.catalog.command;

import com.mentormatching.mentor.entity.GradeLevelGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CreateGradeCommand(
        @NotBlank(message = "Grade name cannot be blank")
        String name,

        @NotNull(message = "Level group is required")
        GradeLevelGroup levelGroup
) {
}

