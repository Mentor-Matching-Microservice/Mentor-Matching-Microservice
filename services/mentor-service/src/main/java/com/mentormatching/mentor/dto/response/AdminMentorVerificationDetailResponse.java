package com.mentormatching.mentor.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.mentormatching.mentor.entity.MentorApprovalStatus;
import com.mentormatching.mentor.entity.MentorVerificationStatus;
import lombok.Builder;

/** Chi tiết giấy tờ xác minh dành riêng cho admin khi review ứng viên mentor. */
@Builder
public record AdminMentorVerificationDetailResponse(
        Long id,
        Long mentorId,
        UUID userId,
        String accountFullName,
        MentorApprovalStatus approvalStatus,
        String approvalNote,
        String fullName,
        String idCardNumber,
        String idCardFrontUrl,
        String idCardBackUrl,
        String selfieWithIdUrl,
        MentorVerificationStatus verificationStatus,
        UUID verifiedBy,
        Instant verifiedAt,
        String rejectionReason,
        Instant createdAt,
        Instant updatedAt) {

    @Override
    public String toString() {
        return "AdminMentorVerificationDetailResponse[redacted]";
    }
}
