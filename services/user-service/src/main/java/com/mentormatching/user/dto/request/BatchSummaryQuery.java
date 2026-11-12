package com.mentormatching.user.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Tra cứu nhiều người dùng một lần (tối đa 100 id). */
public record BatchSummaryQuery(@NotEmpty @Size(max = 100) List<@NotNull UUID> userIds) {
}
