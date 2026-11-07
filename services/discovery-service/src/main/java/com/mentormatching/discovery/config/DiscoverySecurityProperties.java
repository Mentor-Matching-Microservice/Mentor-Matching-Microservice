package com.mentormatching.discovery.config;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tài khoản nội bộ dùng chung cho các service đăng ký vào Eureka và cho người vận hành xem registry.
 * <p>
 * Giá trị bắt buộc lấy từ biến môi trường {@code DISCOVERY_USERNAME} và {@code DISCOVERY_PASSWORD}.
 * Lỗi cấu hình được báo bằng thông điệp cố định, không lặp lại giá trị đã nhập để tránh lộ secret trong log.
 *
 * @param username tên đăng nhập HTTP Basic
 * @param password mật khẩu HTTP Basic
 */
@ConfigurationProperties(prefix = "discovery.security")
public record DiscoverySecurityProperties(String username, String password) {

    static final int MIN_PASSWORD_LENGTH = 16;
    /** BCrypt chỉ xử lý tối đa 72 byte. */
    static final int MAX_PASSWORD_BYTES = 72;
    static final int MAX_USERNAME_LENGTH = 64;

    public DiscoverySecurityProperties {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "discovery.security.username must be set (environment variable DISCOVERY_USERNAME)");
        }
        if (username.length() > MAX_USERNAME_LENGTH || !username.chars().allMatch(DiscoverySecurityProperties::isUsernameChar)) {
            throw new IllegalArgumentException("discovery.security.username must be 1-" + MAX_USERNAME_LENGTH
                    + " characters of letters, digits, '.', '_' or '-' (':' is not allowed in HTTP Basic usernames)");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                    "discovery.security.password must be set (environment variable DISCOVERY_PASSWORD)");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "discovery.security.password must contain at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new IllegalArgumentException(
                    "discovery.security.password must not exceed " + MAX_PASSWORD_BYTES + " bytes");
        }
        if (password.toLowerCase(Locale.ROOT).startsWith("replace-with")) {
            throw new IllegalArgumentException(
                    "discovery.security.password still uses the .env.example placeholder; set a real secret");
        }
    }

    private static boolean isUsernameChar(int c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                || c == '.' || c == '_' || c == '-';
    }

    /** Không đưa mật khẩu vào log, thông báo lỗi hay output debug. */
    @Override
    public String toString() {
        return "DiscoverySecurityProperties[username=" + username + ", password=******]";
    }
}
