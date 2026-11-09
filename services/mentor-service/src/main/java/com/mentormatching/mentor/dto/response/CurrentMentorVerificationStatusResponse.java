package com.mentormatching.mentor.dto.response;

import com.mentormatching.mentor.entity.MentorVerificationStatus;
import lombok.Builder;

/** Trạng thái KYC của tài khoản hiện tại; status null khi chưa nộp. */
@Builder
public record CurrentMentorVerificationStatusResponse(
        MentorVerificationStatus verificationStatus,
        String rejectionReason) {
}
