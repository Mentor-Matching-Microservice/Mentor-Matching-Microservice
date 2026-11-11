package com.mentormatching.mentor.dto.catalog.command;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record DeleteSubjectCommand(
        @NotNull(message = "Subject ID is required")
        Long id
) {
}

