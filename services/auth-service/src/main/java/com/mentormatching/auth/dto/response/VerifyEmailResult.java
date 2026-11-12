package com.mentormatching.auth.dto.response;

import java.util.UUID;

import com.mentormatching.auth.entity.AccountStatus;

/**
 * Kết quả xác minh email: trạng thái tài khoản sau khi xác minh (thường là {@code ACTIVE}).
 */
public record VerifyEmailResult(UUID accountId, AccountStatus status) {
}
