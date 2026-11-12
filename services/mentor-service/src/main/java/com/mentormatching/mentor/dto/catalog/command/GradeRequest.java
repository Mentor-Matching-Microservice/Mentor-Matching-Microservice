package com.mentormatching.mentor.dto.catalog.command;

import com.mentormatching.mentor.entity.GradeLevelGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GradeRequest(
        @NotBlank(message = "Grade name cannot be blank")
        @Size(max = 50, message = "Grade name cannot exceed 50 characters")
        String name,

        @NotNull(message = "Level group is required")
        GradeLevelGroup levelGroup
) {
}

