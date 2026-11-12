package com.mentormatching.auth.service;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Thời hạn token một lần và khoảng chờ tối thiểu giữa hai lần gửi email cho cùng tài khoản.
 *
 * @param emailVerificationTtl thời hạn link xác minh email (mặc định 24 giờ)
 * @param passwordResetTtl     thời hạn link đặt lại mật khẩu (mặc định 30 phút)
 * @param resendCooldown       chờ tối thiểu giữa hai lần gửi email cho cùng tài khoản (mặc định 60 giây)
 */
@ConfigurationProperties(prefix = "auth.tokens")
public record AccountTokenProperties(Duration emailVerificationTtl, Duration passwordResetTtl, Duration resendCooldown) {

    public AccountTokenProperties {
        emailVerificationTtl = emailVerificationTtl == null ? Duration.ofHours(24) : emailVerificationTtl;
        passwordResetTtl = passwordResetTtl == null ? Duration.ofMinutes(30) : passwordResetTtl;
        resendCooldown = resendCooldown == null ? Duration.ofSeconds(60) : resendCooldown;
        if (emailVerificationTtl.isNegative() || emailVerificationTtl.isZero() || emailVerificationTtl.toDays() > 7) {
            throw new IllegalArgumentException("auth.tokens.email-verification-ttl must be between 1s and 7 days");
        }
        if (passwordResetTtl.isNegative() || passwordResetTtl.isZero() || passwordResetTtl.toHours() > 24) {
            throw new IllegalArgumentException("auth.tokens.password-reset-ttl must be between 1s and 24 hours");
        }
        if (resendCooldown.isNegative()) {
            throw new IllegalArgumentException("auth.tokens.resend-cooldown must not be negative");
        }
    }
}
