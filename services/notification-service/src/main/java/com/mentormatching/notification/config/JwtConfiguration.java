package com.mentormatching.notification.config;

import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Xác thực access token RS256 do auth-service ký, cùng hợp đồng claims với user-service:
 * chữ ký RS256, issuer, audience, exp, subject là UUID (accountId) và claim {@code roles}
 * là danh sách chuỗi không rỗng. Chỉ cần public key, không gọi sang auth-service.
 */
@Configuration(proxyBeanMethods = false)
public class JwtConfiguration {

    static final int MIN_RSA_KEY_BITS = 2048;

    @Bean
    JwtDecoder jwtDecoder(ResourceLoader resourceLoader,
                          @Value("${notification.jwt.public-key-location:}") String publicKeyLocation,
                          @Value("${notification.jwt.issuer:}") String issuer,
                          @Value("${notification.jwt.audience:}") String audience) throws Exception {
        // Thiếu cấu hình bắt buộc thì dừng khởi động với thông báo chỉ tên biến môi trường.
        requireText(publicKeyLocation, "notification.jwt.public-key-location (AUTH_JWT_PUBLIC_KEY_LOCATION)");
        requireText(issuer, "notification.jwt.issuer (AUTH_JWT_ISSUER)");
        requireText(audience, "notification.jwt.audience (AUTH_JWT_AUDIENCE)");

        Resource keyResource = resourceLoader.getResource(publicKeyLocation);
        if (!keyResource.exists()) {
            throw new IllegalStateException("JWT public key not found at the configured AUTH_JWT_PUBLIC_KEY_LOCATION");
        }
        RSAPublicKey key;
        try (var input = keyResource.getInputStream()) {
            key = RsaKeyConverters.x509().convert(input);
        }
        if (key == null || key.getModulus().bitLength() < MIN_RSA_KEY_BITS) {
            throw new IllegalArgumentException("RSA public key must be at least " + MIN_RSA_KEY_BITS + " bits");
        }

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(key)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), claimsContract(audience)));
        return decoder;
    }

    /**
     * Kiểm tra claims riêng của hệ thống sau khi chữ ký, issuer và thời hạn đã hợp lệ.
     * Mọi vi phạm đều trả cùng một lỗi chung để không tiết lộ claim nào sai.
     */
    static OAuth2TokenValidator<Jwt> claimsContract(String audience) {
        return jwt -> {
            try {
                String subject = jwt.getSubject();
                boolean validSubject = subject != null
                        && UUID.fromString(subject).toString().equalsIgnoreCase(subject);
                Object roles = jwt.getClaims().get("roles");
                boolean validRoles = roles instanceof List<?> list
                        && list.stream().allMatch(role -> role instanceof String value && !value.isBlank());
                if (validSubject && validRoles && jwt.getExpiresAt() != null
                        && jwt.getAudience() != null && jwt.getAudience().contains(audience)) {
                    return OAuth2TokenValidatorResult.success();
                }
            } catch (IllegalArgumentException ignored) {
                // Subject không phải UUID: coi là lỗi xác thực.
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid token claims", null));
        };
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be set");
        }
    }
}
