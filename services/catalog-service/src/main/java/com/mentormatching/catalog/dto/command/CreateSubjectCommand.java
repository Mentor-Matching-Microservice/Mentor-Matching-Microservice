package com.mentormatching.catalog.dto.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CreateSubjectCommand(@NotNull @Positive Long categoryId, @NotBlank @Size(max = 255) String name, String description) {
    public CreateSubjectCommand {
        name = name == null ? null : name.trim();
        description = description == null ? null : description.trim();
    }
}
