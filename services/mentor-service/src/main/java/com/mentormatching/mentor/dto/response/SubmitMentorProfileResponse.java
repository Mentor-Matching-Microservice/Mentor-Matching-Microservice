package com.mentormatching.mentor.dto.response;

import java.time.Instant;

import com.mentormatching.mentor.entity.MentorApprovalStatus;
import lombok.Builder;

@Builder
public record SubmitMentorProfileResponse(
        Long mentorId,
        MentorApprovalStatus approvalStatus,
        Instant submittedAt,
        String message) {
}
