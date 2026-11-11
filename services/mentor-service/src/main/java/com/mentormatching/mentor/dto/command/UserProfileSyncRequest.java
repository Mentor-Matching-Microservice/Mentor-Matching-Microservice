package com.mentormatching.mentor.dto.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record UserProfileSyncRequest(
        @NotBlank String fullName,
        String avatarUrl,
        Long avatarMediaId
) {
}

