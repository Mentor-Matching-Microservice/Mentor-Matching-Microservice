package com.mentormatching.auth.dto.response;

import java.util.UUID;

import com.mentormatching.auth.entity.AccountStatus;

/**
 * Trạng thái tài khoản sau khi đổi. {@code aggregateVersion} tăng theo mỗi thay đổi của tài khoản.
 *
 * @param revokedRefreshTokens số phiên đăng nhập (refresh token) vừa bị thu hồi
 */
public record AccountStatusResult(UUID accountId, AccountStatus status, long aggregateVersion, int revokedRefreshTokens) {
}
