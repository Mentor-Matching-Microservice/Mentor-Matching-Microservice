package com.mentormatching.auth.dto.request;

import com.mentormatching.auth.entity.AccountStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Admin đổi trạng thái tài khoản.
 *
 * @param status trạng thái mới: {@code ACTIVE}, {@code LOCKED} hoặc {@code DISABLED}
 * @param reason lý do (tuỳ chọn, tối đa 300 ký tự), được gửi kèm sự kiện {@code AccountStatusChanged}
 */
public record ChangeAccountStatusCommand(@NotNull AccountStatus status, @Size(max = 300) String reason) {
}
