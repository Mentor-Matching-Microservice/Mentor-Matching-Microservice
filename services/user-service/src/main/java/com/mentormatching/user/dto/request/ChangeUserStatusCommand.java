package com.mentormatching.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Admin đổi trạng thái tài khoản của người dùng (được chuyển cho auth-service, nơi sở hữu trạng thái).
 *
 * @param status {@code ACTIVE}, {@code LOCKED} hoặc {@code DISABLED}
 * @param reason lý do (tuỳ chọn)
 */
public record ChangeUserStatusCommand(
        @NotBlank @Pattern(regexp = "ACTIVE|LOCKED|DISABLED") String status,
        @Size(max = 300) String reason) {
}
