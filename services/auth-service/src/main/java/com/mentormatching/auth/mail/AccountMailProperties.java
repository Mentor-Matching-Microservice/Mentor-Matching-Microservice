package com.mentormatching.auth.mail;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình email tài khoản.
 *
 * @param enabled              true: gửi qua SMTP ({@code spring.mail.*}); false: chỉ ghi log, không gửi
 * @param from                 địa chỉ người gửi
 * @param verifyEmailUrl       trang frontend nhận {@code ?token=} để gọi {@code POST /api/v1/auth/verify-email}
 * @param resetPasswordUrl     trang frontend nhận {@code ?token=} để gọi {@code POST /api/v1/auth/reset-password}
 * @param logLinks             CHỈ dùng khi phát triển local và {@code enabled=false}: ghi link (chứa token) ra log
 */
@ConfigurationProperties(prefix = "auth.mail")
public record AccountMailProperties(Boolean enabled, String from, URI verifyEmailUrl, URI resetPasswordUrl,
                                    Boolean logLinks) {

    public AccountMailProperties {
        enabled = enabled != null && enabled;
        from = from == null || from.isBlank() ? "no-reply@mentor-matching.local" : from.strip();
        verifyEmailUrl = verifyEmailUrl == null ? URI.create("http://localhost:5173/verify-email") : verifyEmailUrl;
        resetPasswordUrl = resetPasswordUrl == null ? URI.create("http://localhost:5173/reset-password") : resetPasswordUrl;
        logLinks = logLinks != null && logLinks;
        if (enabled && logLinks) {
            throw new IllegalArgumentException("auth.mail.log-links must not be enabled together with real mail delivery");
        }
    }

    public String verifyLink(String rawToken) {
        return withToken(verifyEmailUrl, rawToken);
    }

    public String resetLink(String rawToken) {
        return withToken(resetPasswordUrl, rawToken);
    }

    private static String withToken(URI base, String rawToken) {
        String url = base.toString();
        return url + (url.contains("?") ? "&" : "?") + "token=" + rawToken;
    }
}
