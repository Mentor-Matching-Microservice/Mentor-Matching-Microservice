package com.mentormatching.notification.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.mentormatching.notification.support.TestJwtKeys;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

class JwtConfigurationTest {

    private static final TestJwtKeys KEYS = TestJwtKeys.trusted();

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(JwtConfiguration.class)
            .withPropertyValues(
                    "notification.jwt.public-key-location=" + KEYS.publicKeyLocation(),
                    "notification.jwt.issuer=" + TestJwtKeys.ISSUER,
                    "notification.jwt.audience=" + TestJwtKeys.AUDIENCE);

    @Test
    void acceptsTokenIssuedLikeAuthService() {
        UUID userId = UUID.randomUUID();
        runner.run(context -> {
            Jwt jwt = context.getBean(JwtDecoder.class).decode(KEYS.token(userId, "LEARNER", "MENTOR"));
            assertThat(jwt.getSubject()).isEqualTo(userId.toString());
            assertThat(jwt.getClaimAsStringList("roles")).containsExactly("LEARNER", "MENTOR");
        });
    }

    @Test
    void rejectsWrongAudience() {
        assertRejected(KEYS.token(c -> c.audience(List.of("another-api"))));
    }

    @Test
    void rejectsWrongIssuer() {
        assertRejected(KEYS.token(c -> c.issuer("someone-else")));
    }

    @Test
    void rejectsExpiredToken() {
        Instant past = Instant.now().minusSeconds(3600);
        assertRejected(KEYS.token(c -> c.issuedAt(past.minusSeconds(60)).expiresAt(past)));
    }

    @Test
    void rejectsTokenWithoutExpiry() {
        assertRejected(KEYS.token(c -> c.claims(claims -> claims.remove("exp"))));
    }

    @Test
    void rejectsNonUuidSubject() {
        assertRejected(KEYS.token(c -> c.subject("not-a-uuid")));
    }

    @Test
    void rejectsMissingRoles() {
        assertRejected(KEYS.token(c -> c.claims(claims -> claims.remove("roles"))));
    }

    @Test
    void rejectsBlankOrNonStringRoles() {
        assertRejected(KEYS.token(c -> c.claim("roles", List.of("LEARNER", " "))));
        assertRejected(KEYS.token(c -> c.claim("roles", "LEARNER")));
        assertRejected(KEYS.token(c -> c.claim("roles", List.of(1, 2))));
    }

    @Test
    void rejectsTokenSignedByAnotherKey() {
        assertRejected(TestJwtKeys.untrusted(2048).token(UUID.randomUUID(), "LEARNER"));
    }

    @Test
    void rejectsGarbage() {
        assertRejected("not.a.jwt");
    }

    @Test
    void failsStartupWhenPublicKeyLocationMissing() {
        runner.withPropertyValues("notification.jwt.public-key-location=").run(context ->
                assertThat(context).getFailure().rootCause()
                        .hasMessageContaining("AUTH_JWT_PUBLIC_KEY_LOCATION"));
    }

    @Test
    void failsStartupWhenAudienceMissing() {
        runner.withPropertyValues("notification.jwt.audience= ").run(context ->
                assertThat(context).getFailure().rootCause()
                        .hasMessageContaining("AUTH_JWT_AUDIENCE"));
    }

    @Test
    void failsStartupWhenIssuerMissing() {
        runner.withPropertyValues("notification.jwt.issuer=").run(context ->
                assertThat(context).getFailure().rootCause()
                        .hasMessageContaining("AUTH_JWT_ISSUER"));
    }

    @Test
    void failsStartupWhenKeyFileDoesNotExist() {
        runner.withPropertyValues("notification.jwt.public-key-location=file:./does-not-exist/public.pem")
                .run(context -> assertThat(context).getFailure().rootCause()
                        .hasMessageContaining("JWT public key not found"));
    }

    @Test
    void failsStartupWhenKeyIsTooSmall() {
        TestJwtKeys weak = TestJwtKeys.untrusted(1024);
        runner.withPropertyValues("notification.jwt.public-key-location=" + weak.publicKeyLocation())
                .run(context -> assertThat(context).getFailure().rootCause()
                        .hasMessageContaining("at least 2048 bits"));
    }

    private void assertRejected(String token) {
        runner.run(context -> {
            JwtDecoder decoder = context.getBean(JwtDecoder.class);
            assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
        });
    }
}
