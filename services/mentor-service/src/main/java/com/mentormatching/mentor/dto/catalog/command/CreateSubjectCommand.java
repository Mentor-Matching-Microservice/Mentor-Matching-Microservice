package com.mentormatching.mentor.dto.catalog.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CreateSubjectCommand(
        @NotNull(message = "Category ID is required")
        Long categoryId,

        @NotBlank(message = "Subject name cannot be blank")
        String name,

        String description
) {
}

