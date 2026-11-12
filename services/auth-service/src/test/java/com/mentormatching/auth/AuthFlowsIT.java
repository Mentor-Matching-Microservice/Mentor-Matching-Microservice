package com.mentormatching.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.UUID;

import com.mentormatching.auth.entity.AccountStatus;
import com.mentormatching.auth.repository.AccountRepository;
import com.mentormatching.auth.repository.EmailVerificationTokenRepository;
import com.mentormatching.auth.repository.PasswordResetTokenRepository;
import com.mentormatching.auth.support.AuthIntegrationTestSupport;
import com.mentormatching.auth.support.CapturingMailSender;
import com.mentormatching.auth.support.CapturingMailSender.Kind;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import tools.jackson.databind.JsonNode;

/**
 * #63 trên MySQL 8.4 thật: JWKS, xác minh email, quên/đặt lại mật khẩu, xoay vòng refresh token theo họ.
 */
class AuthFlowsIT extends AuthIntegrationTestSupport {

    @Autowired private CapturingMailSender mails;
    @Autowired private AccountRepository accounts;
    @Autowired private EmailVerificationTokenRepository verificationTokens;
    @Autowired private PasswordResetTokenRepository resetTokens;

    // ---------- JWKS ----------

    @Test
    void jwksPublishesOnlyThePublicKeyThatSignsAccessTokens() {
        String email = activeAccount();
        String accessToken = json(post("/api/v1/auth/login", loginBody(email, PASSWORD))).get("data").get("accessToken").asString();

        HttpResponse<String> jwks = get("/.well-known/jwks.json");

        assertThat(jwks.statusCode()).isEqualTo(200);
        assertThat(jwks.headers().firstValue("Cache-Control")).hasValueSatisfying(v -> assertThat(v).contains("max-age=300"));
        JsonNode key = json(jwks).get("keys").get(0);
        assertThat(key.get("kty").asString()).isEqualTo("RSA");
        assertThat(key.get("use").asString()).isEqualTo("sig");
        assertThat(key.get("alg").asString()).isEqualTo("RS256");
        for (String privatePart : new String[] {"d", "p", "q", "dp", "dq", "qi"}) {
            assertThat(key.has(privatePart)).as("private member " + privatePart).isFalse();
        }
        // Một resource server chỉ cần URL JWKS là kiểm tra được chữ ký và kid.
        Jwt jwt = NimbusJwtDecoder.withJwkSetUri(baseUrl() + "/.well-known/jwks.json").build().decode(accessToken);
        assertThat(jwt.getHeaders().get("kid")).isEqualTo(key.get("kid").asString());
        assertThat(jwt.getAudience()).containsExactly(AUDIENCE);
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("LEANER");
        assertThat(UUID.fromString(jwt.getSubject())).isNotNull();
    }

    @Test
    void jwksIsPublic() {
        assertThat(send(request("/.well-known/jwks.json").GET().build()).statusCode()).isEqualTo(200);
    }

    // ---------- verify email ----------

    @Test
    void registrationSendsAVerificationLinkAndLoginNeedsAVerifiedEmail() {
        String email = uniqueEmail();
        HttpResponse<String> registered = post("/api/v1/auth/register", registerBody(email, PASSWORD));
        assertThat(registered.statusCode()).isEqualTo(201);
        assertThat(json(registered).get("data").get("status").asString()).isEqualTo("PENDING_VERIFICATION");
        String token = mails.latestToken(email, Kind.VERIFY_EMAIL);

        assertThat(post("/api/v1/auth/login", loginBody(email, PASSWORD)).statusCode()).as("pending").isEqualTo(401);

        HttpResponse<String> verified = post("/api/v1/auth/verify-email", "{\"token\":\"" + token + "\"}");
        assertThat(verified.statusCode()).isEqualTo(200);
        assertThat(json(verified).get("data").get("status").asString()).isEqualTo("ACTIVE");
        assertThat(post("/api/v1/auth/login", loginBody(email, PASSWORD)).statusCode()).isEqualTo(200);

        HttpResponse<String> again = post("/api/v1/auth/verify-email", "{\"token\":\"" + token + "\"}");
        assertProblem(again, 400, "INVALID_VERIFICATION_TOKEN");
    }

    @Test
    void expiredOrUnknownVerificationTokensAreRejected() {
        String email = uniqueEmail();
        post("/api/v1/auth/register", registerBody(email, PASSWORD));
        String token = mails.latestToken(email, Kind.VERIFY_EMAIL);
        verificationTokens.findAll().stream().filter(t -> t.getEmailToVerify().equals(email)).forEach(t -> {
            t.setExpiresAt(Instant.now().minusSeconds(1));
            verificationTokens.save(t);
        });

        assertProblem(post("/api/v1/auth/verify-email", "{\"token\":\"" + token + "\"}"), 400, "INVALID_VERIFICATION_TOKEN");
        assertProblem(post("/api/v1/auth/verify-email", "{\"token\":\"" + "x".repeat(43) + "\"}"), 400, "INVALID_VERIFICATION_TOKEN");
        assertProblem(post("/api/v1/auth/verify-email", "{\"token\":\"short\"}"), 400, "INVALID_VERIFICATION_TOKEN");
        assertThat(accounts.findByLoginEmail(email).orElseThrow().getStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
    }

    @Test
    void resendVerificationNeverRevealsWhetherTheEmailExists() {
        String pending = uniqueEmail();
        post("/api/v1/auth/register", registerBody(pending, PASSWORD));
        String active = activeAccount();
        mails.clear();

        assertThat(post("/api/v1/auth/verify-email/resend", "{\"email\":\"" + uniqueEmail() + "\"}").statusCode()).isEqualTo(202);
        assertThat(post("/api/v1/auth/verify-email/resend", "{\"email\":\"" + active + "\"}").statusCode()).isEqualTo(202);
        assertThat(post("/api/v1/auth/verify-email/resend", "{\"email\":\"" + pending.toUpperCase() + "\"}").statusCode()).isEqualTo(202);

        assertThat(mails.to(pending, Kind.VERIFY_EMAIL)).hasSize(1);
        assertThat(mails.to(active, Kind.VERIFY_EMAIL)).isEmpty();
    }

    // ---------- forgot / reset password ----------

    @Test
    void passwordResetChangesThePasswordAndSignsOutEverywhere() {
        String email = activeAccount();
        String refreshBefore = refreshCookie(post("/api/v1/auth/login", loginBody(email, PASSWORD)));
        mails.clear();

        assertThat(post("/api/v1/auth/forgot-password", "{\"email\":\"" + email + "\"}").statusCode()).isEqualTo(202);
        String token = mails.latestToken(email, Kind.RESET_PASSWORD);
        String newPassword = "N3w-Passw0rd-Reset";

        assertProblem(post("/api/v1/auth/reset-password", resetBody(token, newPassword, "different")), 400, "PASSWORD_MISMATCH");
        HttpResponse<String> reset = post("/api/v1/auth/reset-password", resetBody(token, newPassword, newPassword));
        assertThat(reset.statusCode()).isEqualTo(200);

        assertThat(post("/api/v1/auth/login", loginBody(email, PASSWORD)).statusCode()).as("old password").isEqualTo(401);
        assertThat(post("/api/v1/auth/login", loginBody(email, newPassword)).statusCode()).as("new password").isEqualTo(200);
        assertThat(post("/api/v1/auth/refresh-token", null, refreshBefore).statusCode()).as("old sessions revoked").isEqualTo(401);
        assertProblem(post("/api/v1/auth/reset-password", resetBody(token, "An0ther-Passw0rd", "An0ther-Passw0rd")), 400,
                "INVALID_RESET_TOKEN");
    }

    @Test
    void forgotPasswordAnswersTheSameForUnknownEmailsAndLockedAccounts() {
        String locked = activeAccount();
        accounts.findByLoginEmail(locked).ifPresent(a -> {
            a.setStatus(AccountStatus.LOCKED);
            accounts.save(a);
        });
        mails.clear();

        assertThat(post("/api/v1/auth/forgot-password", "{\"email\":\"" + uniqueEmail() + "\"}").statusCode()).isEqualTo(202);
        assertThat(post("/api/v1/auth/forgot-password", "{\"email\":\"" + locked + "\"}").statusCode()).isEqualTo(202);
        assertThat(mails.to(locked, Kind.RESET_PASSWORD)).isEmpty();
    }

    @Test
    void expiredResetTokenAndOldLinksAreRejected() {
        String email = activeAccount();
        post("/api/v1/auth/forgot-password", "{\"email\":\"" + email + "\"}");
        String first = mails.latestToken(email, Kind.RESET_PASSWORD);
        post("/api/v1/auth/forgot-password", "{\"email\":\"" + email + "\"}");
        String second = mails.latestToken(email, Kind.RESET_PASSWORD);

        assertThat(post("/api/v1/auth/reset-password", resetBody(second, "Brand-N3w-Pass", "Brand-N3w-Pass")).statusCode()).isEqualTo(200);
        assertProblem(post("/api/v1/auth/reset-password", resetBody(first, "Brand-N3w-Pass2", "Brand-N3w-Pass2")), 400,
                "INVALID_RESET_TOKEN");

        post("/api/v1/auth/forgot-password", "{\"email\":\"" + email + "\"}");
        String third = mails.latestToken(email, Kind.RESET_PASSWORD);
        resetTokens.findAll().forEach(t -> {
            t.setExpiresAt(Instant.now().minusSeconds(1));
            resetTokens.save(t);
        });
        assertProblem(post("/api/v1/auth/reset-password", resetBody(third, "Brand-N3w-Pass3", "Brand-N3w-Pass3")), 400,
                "INVALID_RESET_TOKEN");
    }

    @Test
    void publicPostsRequireTheFrontendOrigin() {
        HttpRequest noOrigin = request("/api/v1/auth/forgot-password").header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"a@b.co\"}")).build();
        assertThat(send(noOrigin).statusCode()).isEqualTo(403);
        HttpRequest evil = request("/api/v1/auth/reset-password").header("Origin", "https://evil.example")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(resetBody("x".repeat(43), "Passw0rd!!", "Passw0rd!!"))).build();
        assertThat(send(evil).statusCode()).isEqualTo(403);
    }

    // ---------- refresh token family ----------

    @Test
    void reusingARotatedRefreshTokenRevokesTheWholeFamily() {
        String email = activeAccount();
        String first = refreshCookie(post("/api/v1/auth/login", loginBody(email, PASSWORD)));
        HttpResponse<String> rotated = post("/api/v1/auth/refresh-token", null, first);
        assertThat(rotated.statusCode()).isEqualTo(200);
        String second = refreshCookie(rotated);
        assertThat(second).isNotEqualTo(first);

        assertProblem(post("/api/v1/auth/refresh-token", null, first), 401, "INVALID_REFRESH_TOKEN");
        assertThat(post("/api/v1/auth/refresh-token", null, second).statusCode()).as("stolen family revoked").isEqualTo(401);

        // Một phiên đăng nhập khác (họ token khác) không bị ảnh hưởng.
        String otherSession = refreshCookie(post("/api/v1/auth/login", loginBody(email, PASSWORD)));
        assertThat(post("/api/v1/auth/refresh-token", null, otherSession).statusCode()).isEqualTo(200);
    }

    @Test
    void lockedOrDisabledAccountsCannotLoginOrRefresh() {
        String email = activeAccount();
        String refresh = refreshCookie(post("/api/v1/auth/login", loginBody(email, PASSWORD)));
        accounts.findByLoginEmail(email).ifPresent(a -> {
            a.setStatus(AccountStatus.DISABLED);
            accounts.save(a);
        });

        assertProblem(post("/api/v1/auth/login", loginBody(email, PASSWORD)), 401, "INVALID_CREDENTIALS");
        assertProblem(post("/api/v1/auth/refresh-token", null, refresh), 401, "INVALID_REFRESH_TOKEN");
    }

    // ---------- internal mentor role (build fix) ----------

    @Test
    void internalMentorRoleGrantIsIdempotent() {
        String email = activeAccount();
        UUID accountId = accounts.findByLoginEmail(email).orElseThrow().getId();
        HttpRequest grant = request("/internal/accounts/" + accountId + "/mentor-role")
                .header("Authorization", "Bearer " + INTERNAL_TOKEN).PUT(HttpRequest.BodyPublishers.noBody()).build();

        assertThat(send(grant).statusCode()).isEqualTo(204);
        assertThat(send(grant).statusCode()).isEqualTo(204);
        String access = json(post("/api/v1/auth/login", loginBody(email, PASSWORD))).get("data").get("accessToken").asString();
        Jwt jwt = NimbusJwtDecoder.withJwkSetUri(baseUrl() + "/.well-known/jwks.json").build().decode(access);
        assertThat(jwt.getClaimAsStringList("roles")).containsExactlyInAnyOrder("LEANER", "MENTOR");

        HttpRequest unknown = request("/internal/accounts/" + UUID.randomUUID() + "/mentor-role")
                .header("Authorization", "Bearer " + INTERNAL_TOKEN).PUT(HttpRequest.BodyPublishers.noBody()).build();
        assertThat(send(unknown).statusCode()).isEqualTo(404);
        HttpRequest noToken = request("/internal/accounts/" + accountId + "/mentor-role")
                .PUT(HttpRequest.BodyPublishers.noBody()).build();
        assertThat(send(noToken).statusCode()).isEqualTo(401);
    }

    // ---------- helpers ----------

    /** Đăng ký và xác minh email qua đúng luồng công khai; trả về email đã ACTIVE. */
    private String activeAccount() {
        String email = uniqueEmail();
        assertThat(post("/api/v1/auth/register", registerBody(email, PASSWORD)).statusCode()).isEqualTo(201);
        String token = mails.latestToken(email, Kind.VERIFY_EMAIL);
        assertThat(post("/api/v1/auth/verify-email", "{\"token\":\"" + token + "\"}").statusCode()).isEqualTo(200);
        return email;
    }

    private static String resetBody(String token, String password, String confirm) {
        return "{\"token\":\"" + token + "\",\"password\":\"" + password + "\",\"confirmPassword\":\"" + confirm + "\"}";
    }

    private static void assertProblem(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        assertThat(json(response).get("code").asString()).isEqualTo(code);
    }
}
