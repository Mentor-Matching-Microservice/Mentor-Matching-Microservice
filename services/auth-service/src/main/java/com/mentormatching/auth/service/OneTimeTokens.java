package com.mentormatching.auth.service;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Token một lần (xác minh email, đặt lại mật khẩu): 32 byte ngẫu nhiên, Base64URL không padding (43 ký tự),
 * cùng định dạng và cách băm SHA-256 với refresh token. Chỉ hash được lưu trong DB.
 */
public final class OneTimeTokens {

    public static final String FORMAT = "[A-Za-z0-9_-]{43}";
    private static final SecureRandom RANDOM = new SecureRandom();

    private OneTimeTokens() {
    }

    public static String generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static boolean wellFormed(String raw) {
        return raw != null && raw.matches(FORMAT);
    }

    public static String hash(String raw) {
        return TokenService.hashRefreshToken(raw);
    }
}
