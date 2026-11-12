package com.mentormatching.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Token một lần lấy từ link trong email (xác minh email).
 *
 * @param token token thô (43 ký tự Base64URL)
 */
public record OneTimeTokenCommand(@NotBlank @Size(max = 100) String token) {
}
