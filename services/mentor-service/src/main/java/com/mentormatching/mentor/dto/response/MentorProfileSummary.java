package com.mentormatching.mentor.dto.response;

import lombok.Builder;

@Builder
public record MentorProfileSummary(
        Long mentorId,
        String mentorUserId,
        String fullName
) {
}

