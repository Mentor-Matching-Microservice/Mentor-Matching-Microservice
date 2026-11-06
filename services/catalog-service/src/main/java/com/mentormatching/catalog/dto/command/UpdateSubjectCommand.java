package com.mentormatching.catalog.dto.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UpdateSubjectCommand(@NotNull @Positive Long id, @NotNull @Positive Long categoryId, @NotBlank @Size(max = 255) String name, String description) {
    public UpdateSubjectCommand {
        name = name == null ? null : name.trim();
        description = description == null ? null : description.trim();
    }
}
