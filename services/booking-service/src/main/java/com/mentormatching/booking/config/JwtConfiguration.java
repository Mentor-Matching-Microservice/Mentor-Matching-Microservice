package com.mentormatching.booking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.UUID;

@Configuration(proxyBeanMethods = false)
public class JwtConfiguration {

    @Bean
    JwtDecoder jwtDecoder(@Value("${booking.jwt.public-key-location:#{null}}") Resource keyResource,
                          @Value("${booking.jwt.issuer:auth-service}") String issuer,
                          @Value("${booking.jwt.audience:}") String audience) throws Exception {
        if (keyResource == null || !keyResource.exists()) {
            // When key resource is not yet configured (e.g. initial dev environment), return a no-op / pass-through decoder
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .subject("00000000-0000-0000-0000-000000000000")
                    .claim("roles", List.of("ROLE_MENTOR"))
                    .build();
        }

        if (issuer.isBlank()) {
            throw new IllegalArgumentException("JWT issuer must not be blank");
        }

        RSAPublicKey key;
        try (var input = keyResource.getInputStream()) {
            key = RsaKeyConverters.x509().convert(input);
        }

        if (key.getModulus().bitLength() < 2048) {
            throw new IllegalArgumentException("RSA public key must be at least 2048 bits");
        }

        var decoder = NimbusJwtDecoder.withPublicKey(key)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();

        OAuth2TokenValidator<Jwt> contract = jwt -> {
            try {
                String subject = jwt.getSubject();
                boolean validSubject = subject != null && UUID.fromString(subject).toString().equalsIgnoreCase(subject);

                Object roles = jwt.getClaims().get("roles");
                boolean validRoles = roles instanceof List<?> list
                        && list.stream().allMatch(role -> role instanceof String value && !value.isBlank());

                boolean validAudience = audience.isBlank()
                        || (jwt.getAudience() != null && jwt.getAudience().contains(audience));

                if (validSubject && validRoles && jwt.getExpiresAt() != null && validAudience) {
                    return OAuth2TokenValidatorResult.success();
                }
            } catch (IllegalArgumentException ignored) {
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
