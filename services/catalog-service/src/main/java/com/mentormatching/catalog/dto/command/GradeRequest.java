package com.mentormatching.catalog.dto.command;

import com.mentormatching.catalog.entity.GradeLevelGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record GradeRequest(@NotBlank @Size(max = 50) String name, @NotNull GradeLevelGroup levelGroup) {
    public GradeRequest {
        name = name == null ? null : name.trim();
    }
}
