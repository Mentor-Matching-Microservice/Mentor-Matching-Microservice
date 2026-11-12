package com.mentormatching.user.dto.response;

import java.util.UUID;

/** Trạng thái tài khoản sau khi admin đổi (do auth-service trả về). */
public record UserStatusResult(UUID userId, String status, long aggregateVersion) {
}
