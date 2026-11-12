package com.mentormatching.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import com.mentormatching.auth.dto.request.ResetPasswordCommand;
import com.mentormatching.auth.entity.Account;
import com.mentormatching.auth.entity.AccountStatus;
import com.mentormatching.auth.entity.PasswordCredential;
import com.mentormatching.auth.entity.PasswordResetToken;
import com.mentormatching.auth.exception.ApiException;
import com.mentormatching.auth.exception.ErrorCode;
import com.mentormatching.auth.repository.AccountRepository;
import com.mentormatching.auth.repository.PasswordCredentialRepository;
import com.mentormatching.auth.repository.PasswordResetTokenRepository;
import com.mentormatching.auth.repository.RefreshTokenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đặt mật khẩu mới bằng token một lần trong email.
 * <ul>
 *   <li>Token sai/hết hạn/đã dùng, hoặc tài khoản đang {@code LOCKED}/{@code DISABLED}: cùng một lỗi chung.</li>
 *   <li>Thành công: đổi hash mật khẩu, vô hiệu mọi link đặt lại khác và <b>thu hồi mọi refresh token</b>
 *       (đăng xuất mọi thiết bị, kể cả thiết bị của kẻ đã biết mật khẩu cũ).</li>
 *   <li>Mở được link trong email cũng chứng minh sở hữu email: tài khoản chờ xác minh được kích hoạt.</li>
 * </ul>
 */
@Service
public class ResetPasswordUseCase {

    static final int BCRYPT_MAX_BYTES = 72;

    private final PasswordResetTokenRepository resetTokens;
    private final PasswordCredentialRepository credentials;
    private final AccountRepository accounts;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;

    public ResetPasswordUseCase(PasswordResetTokenRepository resetTokens, PasswordCredentialRepository credentials,
                                AccountRepository accounts, RefreshTokenRepository refreshTokens,
                                PasswordEncoder passwordEncoder) {
        this.resetTokens = resetTokens;
        this.credentials = credentials;
        this.accounts = accounts;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void reset(ResetPasswordCommand command) {
        if (command.password().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_PASSWORD, "Password is too long.");
        }
        if (!command.password().equals(command.confirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.PASSWORD_MISMATCH,
                    "Password confirmation does not match.");
        }
        if (!OneTimeTokens.wellFormed(command.token())) {
            throw invalid();
        }
        Instant now = Instant.now();
        PasswordResetToken token = resetTokens.findLockedByTokenHash(OneTimeTokens.hash(command.token()))
                .orElseThrow(ResetPasswordUseCase::invalid);
        Account account = token.getAccount();
        if (token.getConsumedAt() != null || !token.getExpiresAt().isAfter(now)
                || account.getStatus() == AccountStatus.LOCKED || account.getStatus() == AccountStatus.DISABLED) {
            throw invalid();
        }
        PasswordCredential credential = credentials.findById(account.getId())
                .orElseThrow(() -> new IllegalStateException("Account has no password credential"));
        credential.setPasswordHash(passwordEncoder.encode(command.password()));
        credential.setPasswordChangedAt(now);
        credentials.save(credential);

        token.setConsumedAt(now);
        resetTokens.save(token);
        if (account.getStatus() == AccountStatus.PENDING_VERIFICATION) {
            account.setStatus(AccountStatus.ACTIVE);
            account.setEmailVerifiedAt(now);
            accounts.save(account);
        }
        resetTokens.consumeOutstanding(account.getId(), now);
        refreshTokens.revokeAllForAccount(account.getId(), now);
    }

    private static ApiException invalid() {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_RESET_TOKEN,
                "The password reset link is invalid or has expired.");
    }
}
