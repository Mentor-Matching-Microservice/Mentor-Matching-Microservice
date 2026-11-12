package com.mentormatching.mentor.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.mentormatching.mentor.entity.AccountStatus;
import com.mentormatching.mentor.entity.MentorApprovalStatus;
import com.mentormatching.mentor.entity.MentorVerificationStatus;
import lombok.Builder;

@Builder
public record AdminMentorSummaryResponse(
        Long id,
        UUID userId,
        String fullName,
        String headline,
        String avatarUrl,
        MentorApprovalStatus approvalStatus,
        AccountStatus accountStatus,
        MentorVerificationStatus kycStatus,
        long activeSubjectsCount,
        Instant createdAt,
        Instant updatedAt) {
}
