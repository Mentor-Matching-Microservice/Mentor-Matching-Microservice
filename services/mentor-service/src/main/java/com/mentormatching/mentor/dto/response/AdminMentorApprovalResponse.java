package com.mentormatching.mentor.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.mentormatching.mentor.entity.MentorApprovalStatus;
import lombok.Builder;

@Builder
public record AdminMentorApprovalResponse(
        Long mentorId,
        MentorApprovalStatus approvalStatus,
        UUID approvedBy,
        Instant approvedAt,
        String rejectionReason) {
}
