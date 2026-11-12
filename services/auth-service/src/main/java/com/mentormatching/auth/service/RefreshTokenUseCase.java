package com.mentormatching.auth.service;

import com.mentormatching.auth.dto.response.IssuedTokens;
import java.time.Instant;

import com.mentormatching.auth.entity.AccountStatus;
import com.mentormatching.auth.exception.ApiException;
import com.mentormatching.auth.exception.ErrorCode;
import com.mentormatching.auth.repository.AccountRoleRepository;
import com.mentormatching.auth.repository.RefreshTokenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đổi refresh token hợp lệ lấy cặp token mới và thu hồi token cũ.
 */
@Service
public class RefreshTokenUseCase {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RefreshTokenUseCase.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final AccountRoleRepository accountRoleRepository;
    private final TokenService tokenService;

    public RefreshTokenUseCase(RefreshTokenRepository refreshTokenRepository, AccountRoleRepository accountRoleRepository,
                               TokenService tokenService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.accountRoleRepository = accountRoleRepository;
        this.tokenService = tokenService;
    }

    /**
     * Kiểm tra token và xoay vòng trong một transaction.
     *
     * @param rawToken refresh token gốc từ client
     * @return cặp token mới
     */
    @Transactional(noRollbackFor = ApiException.class) // việc thu hồi cả họ token phải được commit dù trả 401
    public IssuedTokens refresh(String rawToken) {

        // Token do service phát hành là 32 byte được mã hóa Base64URL không padding.
        if (rawToken == null || !rawToken.matches("[A-Za-z0-9_-]{43}")) {
            throw invalidToken();
        }

        var current = refreshTokenRepository.findLockedByTokenHash(TokenService.hashRefreshToken(rawToken)).orElseThrow(RefreshTokenUseCase::invalidToken);

        if (current.getRevokedAt() != null) {
            // Token đã xoay hoặc đã thu hồi mà vẫn được gửi lại: có thể đã bị đánh cắp.
            // Thu hồi toàn bộ họ token (cả token mới nhất mà kẻ gian hoặc người dùng đang giữ).
            int revoked = refreshTokenRepository.revokeFamily(current.getFamilyId(), Instant.now());
            log.warn("Refresh token reuse detected accountId={} familyId={} revokedTokens={}",
                    current.getAccount().getId(), current.getFamilyId(), revoked);
            throw invalidToken();
        }
        if (!current.getExpiresAt().isAfter(Instant.now()) || current.getAccount().getStatus() != AccountStatus.ACTIVE) {
            throw invalidToken();
        }

        var roles = accountRoleRepository.findByAccount_Id(current.getAccount().getId()).stream()
                .map(assignment -> assignment.getRole().getCode()).toList();

        return tokenService.rotate(current, roles);
    }

    private static ApiException invalidToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_REFRESH_TOKEN, "Invalid refresh token.");
    }
}
