package com.mentormatching.booking.client.dto;

import lombok.Builder;

@Builder
public record MentorProfileSummary(
        Long mentorId,
        String mentorUserId,
        String fullName
) {
}
