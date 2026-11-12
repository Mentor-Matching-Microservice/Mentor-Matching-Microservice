package com.mentormatching.auth.mail;

import java.time.Instant;

/**
 * Gửi email chứa link một lần cho người dùng. Token thô chỉ tồn tại trong link gửi đi;
 * cơ sở dữ liệu chỉ lưu hash SHA-256 của nó.
 */
public interface AccountMailSender {

    void sendEmailVerification(String email, String rawToken, Instant expiresAt);

    void sendPasswordReset(String email, String rawToken, Instant expiresAt);
}
