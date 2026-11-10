package com.mentormatching.mentor.config;

import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;

/**
 * Cấu hình xác thực JWT do auth-service ký bằng RSA.
 */
@Configuration(proxyBeanMethods = false)
public class JwtConfiguration {

    /**
     * Đọc public key và kiểm tra chữ ký, issuer, audience, thời hạn và định danh.
     */
    @Bean
    JwtDecoder jwtDecoder(@Value("${mentor.jwt.public-key-location}") Resource keyResource,
                          @Value("${mentor.jwt.issuer}") String issuer,
                          @Value("${mentor.jwt.audience}") String audience) throws Exception {

        // Kiểm tra cấu hình bắt buộc
        if (issuer.isBlank() || audience.isBlank()) {
            throw new IllegalArgumentException("JWT issuer and audience must not be blank");
        }

        // Nạp public key từ resource
        RSAPublicKey key;
        try (var input = keyResource.getInputStream()) {
            key = RsaKeyConverters.x509().convert(input);
        }

        if (key.getModulus().bitLength() < 2048) {
            throw new IllegalArgumentException("RSA public key must be at least 2048 bits");
        }

        // Decoder kiểm tra chữ kí RS256
        var decoder = NimbusJwtDecoder.withPublicKey(key)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();

        // Kiểm tra contract claims riêng của hệ thống sau khi giải mã JWT
        OAuth2TokenValidator<Jwt> contract = jwt -> {
            try {
                // Check subject
                String subject = jwt.getSubject();
                boolean validSubject = subject != null && UUID.fromString(subject).toString().equalsIgnoreCase(subject);

                //Check role
                Object roles = jwt.getClaims().get("roles");
                boolean validRoles = roles instanceof List<?> list
                        && list.stream().allMatch(role -> role instanceof String value && !value.isBlank());

                //
                if (validSubject && validRoles && jwt.getExpiresAt() != null
                        && jwt.getAudience() != null && jwt.getAudience().contains(audience)) {
                    return OAuth2TokenValidatorResult.success();
                }
            } catch (IllegalArgumentException ignored) {
                // Invalid identity is an authentication failure.
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid token claims", null));
        };
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer(issuer), contract
                )
        );
        return decoder;
    }
}
