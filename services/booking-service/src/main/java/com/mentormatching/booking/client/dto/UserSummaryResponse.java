package com.mentormatching.booking.client.dto;

import lombok.Builder;

@Builder
public record UserSummaryResponse(
        String userId,
        String fullName,
        String email,
        String phone
) {
}

