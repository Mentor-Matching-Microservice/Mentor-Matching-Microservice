package com.mentormatching.catalog.dto.command;

import com.mentormatching.catalog.entity.GradeLevelGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CreateGradeCommand(@NotBlank @Size(max = 50) String name, @NotNull GradeLevelGroup levelGroup) {
    public CreateGradeCommand {
        name = name == null ? null : name.trim();
    }
}
