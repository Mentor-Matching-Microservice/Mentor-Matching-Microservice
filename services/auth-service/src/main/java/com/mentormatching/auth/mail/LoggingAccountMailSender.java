package com.mentormatching.auth.mail;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dùng khi chưa cấu hình SMTP ({@code AUTH_MAIL_ENABLED=false}): không gửi email, chỉ ghi log rằng có yêu cầu.
 * Mặc định KHÔNG ghi token/link; chỉ khi bật {@code AUTH_MAIL_LOG_LINKS=true} (phát triển local) mới ghi link.
 * Không ghi địa chỉ email đầy đủ.
 */
public class LoggingAccountMailSender implements AccountMailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingAccountMailSender.class);

    private final AccountMailProperties properties;

    public LoggingAccountMailSender(AccountMailProperties properties) {
        this.properties = properties;
        log.warn("Account email delivery is disabled (AUTH_MAIL_ENABLED=false); verification and reset links are not sent");
    }

    @Override
    public void sendEmailVerification(String email, String rawToken, Instant expiresAt) {
        log.info("Email verification requested for {} (delivery disabled)", mask(email));
        if (properties.logLinks()) {
            log.warn("[DEV ONLY] verify-email link: {}", properties.verifyLink(rawToken));
        }
    }

    @Override
    public void sendPasswordReset(String email, String rawToken, Instant expiresAt) {
        log.info("Password reset requested for {} (delivery disabled)", mask(email));
        if (properties.logLinks()) {
            log.warn("[DEV ONLY] reset-password link: {}", properties.resetLink(rawToken));
        }
    }

    static String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + (at >= 0 ? email.substring(at) : "");
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
