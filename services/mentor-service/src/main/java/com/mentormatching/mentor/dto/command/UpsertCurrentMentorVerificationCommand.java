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
 * @param idCardFrontMediaId Media ID mặt trước
 * @param idCardBackUrl URL ảnh mặt sau giấy tờ
 * @param idCardBackMediaId Media ID mặt sau
 * @param selfieWithIdUrl URL ảnh chụp cùng giấy tờ, có thể null
 * @param selfieWithIdMediaId Media ID ảnh chụp cùng giấy tờ
 */
@Builder
public record UpsertCurrentMentorVerificationCommand(
        @NotBlank @Size(max = 255) String fullName,
        @Size(max = 50) String idCardNumber,
        @NotBlank String idCardFrontUrl,
        String idCardFrontMediaId,
        @NotBlank String idCardBackUrl,
        String idCardBackMediaId,
        String selfieWithIdUrl,
        String selfieWithIdMediaId) {

    public UpsertCurrentMentorVerificationCommand {
        fullName = fullName == null ? null : fullName.trim();
    }

    @Override
    public String toString() {
        return "UpsertCurrentMentorVerificationCommand[redacted]";
    }
}
