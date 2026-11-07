package com.mentormatching.mentor.dto.response;

import java.time.Instant;

import com.mentormatching.mentor.entity.MentorVerificationStatus;
import lombok.Builder;

/**
 * Mục trong danh sách admin; không chứa số giấy tờ hoặc URL ảnh xác minh.
 *
 * @param id ID verification để mở chi tiết và review
 * @param mentorId ID hồ sơ mentor
 * @param fullName họ tên ứng viên đã khai khi nộp verification
 * @param verificationStatus trạng thái xác minh hiện tại
 * @param createdAt thời điểm nộp lần đầu
 * @param updatedAt thời điểm cập nhật gần nhất
 */
@Builder
public record AdminMentorVerificationItemResponse(
        Long id,
        Long mentorId,
        String fullName,
        MentorVerificationStatus verificationStatus,
        Instant createdAt,
        Instant updatedAt) {
}
