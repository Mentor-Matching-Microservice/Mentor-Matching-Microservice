package com.mentormatching.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Email của tài khoản (quên mật khẩu, gửi lại link xác minh).
 *
 * @param email email đăng nhập
 */
public record EmailCommand(@NotBlank @Email @Size(max = 254) String email) {
}
