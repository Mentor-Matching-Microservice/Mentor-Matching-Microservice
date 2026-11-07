package com.mentormatching.catalog.dto.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record DeleteGradeCommand(@NotNull @Positive Long id) {
}
