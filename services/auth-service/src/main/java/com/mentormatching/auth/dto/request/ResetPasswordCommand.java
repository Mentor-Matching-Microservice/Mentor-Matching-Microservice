package com.mentormatching.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Đặt mật khẩu mới bằng token trong email.
 *
 * @param token           token thô lấy từ link
 * @param password        mật khẩu mới (8 ký tự trở lên, tối đa 72 byte UTF-8 do giới hạn BCrypt)
 * @param confirmPassword nhập lại mật khẩu mới
 */
public record ResetPasswordCommand(
        @NotBlank @Size(max = 100) String token,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String confirmPassword) {
}
