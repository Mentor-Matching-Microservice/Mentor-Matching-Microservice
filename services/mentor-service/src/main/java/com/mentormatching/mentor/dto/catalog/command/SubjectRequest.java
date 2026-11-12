package com.mentormatching.mentor.dto.catalog.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubjectRequest(
        @NotNull(message = "Category ID is required")
        Long categoryId,

        @NotBlank(message = "Subject name cannot be blank")
        @Size(max = 255, message = "Subject name cannot exceed 255 characters")
        String name,

        String description
) {
}

