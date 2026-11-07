package com.mentormatching.mentor.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.mentormatching.mentor.entity.MentorVerificationStatus;
import lombok.Builder;

/**
 * Kết quả review không chứa số giấy tờ hoặc URL ảnh xác minh.
 *
 * @param id ID verification
 * @param mentorId ID hồ sơ mentor
 * @param status trạng thái sau review
 * @param verifiedBy UUID admin duyệt, null khi từ chối
 * @param verifiedAt thời điểm duyệt, null khi từ chối
 * @param rejectionReason lý do từ chối, null khi duyệt
 */
@Builder
public record MentorVerificationReviewResponse(
        Long id,
        Long mentorId,
        MentorVerificationStatus status,
        UUID verifiedBy,
        Instant verifiedAt,
        String rejectionReason) {
}
