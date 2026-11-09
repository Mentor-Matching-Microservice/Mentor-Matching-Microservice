package com.mentormatching.mentor.dto.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * Thông tin ứng viên nộp để xác minh; userId luôn lấy từ danh tính đã xác thực.
 *
 * @param fullName họ tên trên giấy tờ
 * @param idCardNumber số giấy tờ, có thể null
 * @param idCardFrontUrl URL ảnh mặt trước giấy tờ
 * @param idCardBackUrl URL ảnh mặt sau giấy tờ
 * @param selfieWithIdUrl URL ảnh chụp cùng giấy tờ, có thể null
 */
@Builder
public record UpsertCurrentMentorVerificationCommand(
        @NotBlank @Size(max = 255) String fullName,
        @Size(max = 50) String idCardNumber,
        @NotBlank String idCardFrontUrl,
        @NotBlank String idCardBackUrl,
        String selfieWithIdUrl) {

    public UpsertCurrentMentorVerificationCommand {
        fullName = fullName == null ? null : fullName.trim();
    }

    @Override
    public String toString() {
        return "UpsertCurrentMentorVerificationCommand[redacted]";
    }
}
