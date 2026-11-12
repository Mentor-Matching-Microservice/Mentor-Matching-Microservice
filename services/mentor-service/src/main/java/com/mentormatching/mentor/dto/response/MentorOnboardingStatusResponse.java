package com.mentormatching.mentor.dto.response;

import com.mentormatching.mentor.entity.MentorApprovalStatus;
import com.mentormatching.mentor.entity.MentorVerificationStatus;
import lombok.Builder;

@Builder
public record MentorOnboardingStatusResponse(
        MentorApprovalStatus approvalStatus,
        boolean isBasicProfileCompleted,
        int activeSubjectsCount,
        boolean hasSubmittedKyc,
        MentorVerificationStatus kycStatus,
        boolean canSubmit,
        String rejectionReason) {
}
