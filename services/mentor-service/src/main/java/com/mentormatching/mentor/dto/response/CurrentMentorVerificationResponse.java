package com.mentormatching.mentor.dto.response;

import java.time.Instant;

import com.mentormatching.mentor.entity.MentorVerificationStatus;
import lombok.Builder;

/**
 * Thông tin xác minh chỉ trả cho chủ tài khoản đang đăng nhập.
 *
 * @param id ID bản ghi xác minh
 * @param mentorId ID hồ sơ mentor nháp
 * @param fullName họ tên trên giấy tờ
 * @param idCardNumber số giấy tờ
 * @param idCardFrontUrl URL ảnh mặt trước giấy tờ
 * @param idCardBackUrl URL ảnh mặt sau giấy tờ
 * @param selfieWithIdUrl URL ảnh chụp cùng giấy tờ
 * @param verificationStatus trạng thái xác minh
 * @param rejectionReason lý do từ chối, nếu có
 * @param createdAt thời điểm nộp lần đầu
 * @param updatedAt thời điểm cập nhật gần nhất
 */
@Builder
public record CurrentMentorVerificationResponse(
        Long id,
        Long mentorId,
        String fullName,
        String idCardNumber,
        String idCardFrontUrl,
        String idCardBackUrl,
        String selfieWithIdUrl,
        MentorVerificationStatus verificationStatus,
        String rejectionReason,
        Instant createdAt,
        Instant updatedAt) {

    @Override
    public String toString() {
        return "CurrentMentorVerificationResponse[redacted]";
    }
}
