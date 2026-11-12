package com.mentormatching.auth.service;

import java.time.Instant;

import com.mentormatching.auth.dto.request.OneTimeTokenCommand;
import com.mentormatching.auth.dto.response.VerifyEmailResult;
import com.mentormatching.auth.entity.Account;
import com.mentormatching.auth.entity.AccountStatus;
import com.mentormatching.auth.entity.EmailVerificationToken;
import com.mentormatching.auth.exception.ApiException;
import com.mentormatching.auth.exception.ErrorCode;
import com.mentormatching.auth.repository.AccountRepository;
import com.mentormatching.auth.repository.EmailVerificationTokenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Xác minh email bằng token một lần: tài khoản {@code PENDING_VERIFICATION} chuyển sang {@code ACTIVE}.
 * Token sai, hết hạn, đã dùng hoặc không còn khớp email hiện tại đều trả cùng một lỗi chung.
 * Tài khoản đang {@code LOCKED}/{@code DISABLED} vẫn giữ nguyên trạng thái (chỉ ghi nhận email đã xác minh).
 */
@Service
public class VerifyEmailUseCase {

    private final EmailVerificationTokenRepository tokens;
    private final AccountRepository accounts;

    public VerifyEmailUseCase(EmailVerificationTokenRepository tokens, AccountRepository accounts) {
        this.tokens = tokens;
        this.accounts = accounts;
    }

    @Transactional
    public VerifyEmailResult verify(OneTimeTokenCommand command) {
        if (command == null || !OneTimeTokens.wellFormed(command.token())) {
            throw invalid();
        }
        Instant now = Instant.now();
        EmailVerificationToken token = tokens.findLockedByTokenHash(OneTimeTokens.hash(command.token()))
                .orElseThrow(VerifyEmailUseCase::invalid);
        Account account = token.getAccount();
        if (token.getConsumedAt() != null || !token.getExpiresAt().isAfter(now)
                || !token.getEmailToVerify().equalsIgnoreCase(account.getLoginEmail())) {
            throw invalid();
        }
        token.setConsumedAt(now);
        tokens.save(token);
        if (account.getEmailVerifiedAt() == null) {
            account.setEmailVerifiedAt(now);
        }
        if (account.getStatus() == AccountStatus.PENDING_VERIFICATION) {
            account.setStatus(AccountStatus.ACTIVE);
        }
        accounts.save(account);
        tokens.consumeOutstanding(account.getId(), now);
        return new VerifyEmailResult(account.getId(), account.getStatus());
    }

    private static ApiException invalid() {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_VERIFICATION_TOKEN,
                "The verification link is invalid or has expired.");
    }
}
