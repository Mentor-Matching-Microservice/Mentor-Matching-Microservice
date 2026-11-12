package com.mentormatching.user.dto.response;

import lombok.Builder;

@Builder
public record UserSummaryResponse(
        String userId,
        String fullName,
        String email,
        String phone
) {
}
