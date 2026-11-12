package com.mentormatching.auth.service;

import java.time.Instant;
import java.util.Optional;

import com.mentormatching.auth.entity.Account;
import com.mentormatching.auth.entity.EmailVerificationToken;
import com.mentormatching.auth.entity.PasswordResetToken;
import com.mentormatching.auth.mail.AccountMailSender;
import com.mentormatching.auth.repository.EmailVerificationTokenRepository;
import com.mentormatching.auth.repository.PasswordResetTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Tạo token một lần (lưu hash) và gửi email chứa link SAU KHI transaction commit, để không gửi link cho
 * dữ liệu đã bị rollback. Lỗi gửi email chỉ ghi log, không làm hỏng thao tác chính; người dùng có thể yêu cầu lại.
 */
@Service
@EnableConfigurationProperties(AccountTokenProperties.class)
public class AccountEmailService {

    private static final Logger log = LoggerFactory.getLogger(AccountEmailService.class);

    private final EmailVerificationTokenRepository verificationTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final AccountMailSender mailSender;
    private final AccountTokenProperties properties;

    public AccountEmailService(EmailVerificationTokenRepository verificationTokens,
                               PasswordResetTokenRepository resetTokens, AccountMailSender mailSender,
                               AccountTokenProperties properties) {
        this.verificationTokens = verificationTokens;
        this.resetTokens = resetTokens;
        this.mailSender = mailSender;
        this.properties = properties;
    }

    /** Tạo link xác minh cho email đăng nhập hiện tại; bỏ qua nếu vừa gửi trong khoảng cooldown. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean issueEmailVerification(Account account) {
        Instant now = Instant.now();
        Optional<EmailVerificationToken> latest = verificationTokens.findFirstByAccount_IdOrderByExpiresAtDesc(account.getId());
        if (latest.isPresent() && withinCooldown(latest.get().getExpiresAt(), properties.emailVerificationTtl(), now)) {
            return false;
        }
        String raw = OneTimeTokens.generate();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setAccount(account);
        token.setEmailToVerify(account.getLoginEmail());
        token.setTokenHash(OneTimeTokens.hash(raw));
        token.setExpiresAt(now.plus(properties.emailVerificationTtl()));
        verificationTokens.save(token);
        String email = account.getLoginEmail();
        Instant expiresAt = token.getExpiresAt();
        afterCommit(() -> mailSender.sendEmailVerification(email, raw, expiresAt));
        return true;
    }

    /** Tạo link đặt lại mật khẩu; bỏ qua nếu vừa gửi trong khoảng cooldown. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean issuePasswordReset(Account account) {
        Instant now = Instant.now();
        Optional<PasswordResetToken> latest = resetTokens.findFirstByAccount_IdOrderByExpiresAtDesc(account.getId());
        if (latest.isPresent() && withinCooldown(latest.get().getExpiresAt(), properties.passwordResetTtl(), now)) {
            return false;
        }
        String raw = OneTimeTokens.generate();
        PasswordResetToken token = new PasswordResetToken();
        token.setAccount(account);
        token.setTokenHash(OneTimeTokens.hash(raw));
        token.setExpiresAt(now.plus(properties.passwordResetTtl()));
        resetTokens.save(token);
        String email = account.getLoginEmail();
        Instant expiresAt = token.getExpiresAt();
        afterCommit(() -> mailSender.sendPasswordReset(email, raw, expiresAt));
        return true;
    }

    /** Token được tạo lúc {@code expiresAt - ttl}; còn trong cooldown nếu thời điểm đó gần hơn khoảng chờ. */
    private boolean withinCooldown(Instant latestExpiresAt, java.time.Duration ttl, Instant now) {
        Instant issuedAt = latestExpiresAt.minus(ttl);
        return issuedAt.isAfter(now.minus(properties.resendCooldown()));
    }

    private static void afterCommit(Runnable send) {
        Runnable safe = () -> {
            try {
                send.run();
            } catch (RuntimeException ex) {
                log.warn("Account email could not be sent: {}", ex.getClass().getSimpleName());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safe.run();
                }
            });
        } else {
            safe.run();
        }
    }
}
