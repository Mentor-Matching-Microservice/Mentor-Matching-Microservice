package com.mentormatching.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mentormatching.notification.support.ContractProbeController;
import com.mentormatching.notification.support.NotificationIntegrationTestSupport;
import com.mentormatching.notification.support.TestJwtKeys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;

/**
 * Bảo mật HTTP (Bearer JWT RS256) và hợp đồng lỗi ProblemDetail, chạy trên ứng dụng thật.
 */
class SecurityAndProblemContractIT extends NotificationIntegrationTestSupport {

    @Test
    void healthProbesArePublicAndHideDetails() {
        for (String path : List.of("/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness")) {
            HttpResponse<String> response = get(path);
            assertThat(response.statusCode()).as(path).isEqualTo(200);
            assertThat(json(response).get("status").asString()).isEqualTo("UP");
            assertThat(response.body()).doesNotContain("mongo").doesNotContain("details");
        }
    }

    @Test
    void anonymousRequestGetsProblemDetail401WithBearerChallenge() {
        HttpResponse<String> response = get("/test-probe/me");

        assertProblem(response, 401, "AUTHENTICATION_REQUIRED");
        assertThat(response.headers().firstValue("WWW-Authenticate")).hasValue("Bearer");
        assertThat(json(response).get("instance").asString()).isEqualTo("/test-probe/me");
    }

    @Test
    void validTokenAuthenticatesAsAccountIdWithRoleAuthorities() {
        UUID userId = UUID.randomUUID();
        HttpResponse<String> response = get("/test-probe/me", KEYS.token(userId, "LEARNER", "MENTOR"));

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = json(response);
        assertThat(body.get("userId").asString()).isEqualTo(userId.toString());
        assertThat(body.get("authorities").toString()).contains("ROLE_LEARNER").contains("ROLE_MENTOR");
        assertThat(response.headers().firstValue("Set-Cookie")).as("stateless").isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"garbage", "wrongAudience", "wrongIssuer", "expired", "otherKey", "nonUuidSubject", "noRoles"})
    void invalidTokensAreRejectedWith401(String kind) {
        Instant past = Instant.now().minusSeconds(3600);
        String token = switch (kind) {
            case "garbage" -> "not.a.jwt";
            case "wrongAudience" -> KEYS.token(c -> c.audience(List.of("other-api")));
            case "wrongIssuer" -> KEYS.token(c -> c.issuer("evil-issuer"));
            case "expired" -> KEYS.token(c -> c.issuedAt(past.minusSeconds(60)).expiresAt(past));
            case "otherKey" -> TestJwtKeys.untrusted(2048).token(UUID.randomUUID(), "ADMIN");
            case "nonUuidSubject" -> KEYS.token(c -> c.subject("admin"));
            case "noRoles" -> KEYS.token(c -> c.claims(claims -> claims.remove("roles")));
            default -> throw new IllegalArgumentException(kind);
        };
        HttpResponse<String> response = get("/test-probe/me", token);

        assertProblem(response, 401, "AUTHENTICATION_REQUIRED");
        assertThat(response.body()).doesNotContain(token);
    }

    @Test
    void roleClaimDrivesMethodSecurity() {
        assertProblem(get("/test-probe/admin", KEYS.token(UUID.randomUUID(), "LEARNER")), 403, "ACCESS_DENIED");
        assertThat(get("/test-probe/admin", KEYS.token(UUID.randomUUID(), "ADMIN")).statusCode()).isEqualTo(200);
    }

    @Test
    void businessErrorKeepsItsCodeAndPublicMessage() {
        HttpResponse<String> response = get("/test-probe/business-error", userToken());

        assertProblem(response, 404, "NOTIFICATION_NOT_FOUND");
        assertThat(json(response).get("detail").asString()).isEqualTo("Notification not found.");
        assertThat(json(response).get("type").asString()).isEqualTo("urn:mentor-matching:problem:notification-not-found");
    }

    @Test
    void unexpectedErrorIsGenericAndDoesNotLeakInternals() {
        HttpResponse<String> response = get("/test-probe/unexpected-error", userToken());

        assertProblem(response, 500, "INTERNAL_SERVER_ERROR");
        assertThat(response.body()).doesNotContain(ContractProbeController.INTERNAL_DETAIL)
                .doesNotContain("IllegalStateException").doesNotContain("trace");
    }

    @Test
    void validationErrorListsFieldsWithoutRejectedValues() {
        String secretLikeValue = "x".repeat(25) + "-rejected-value";
        HttpResponse<String> response = send("POST", "/test-probe/validate", userToken(), Map.of(),
                "{\"title\":\"" + secretLikeValue + "\"}");

        assertProblem(response, 400, "VALIDATION_ERROR");
        JsonNode error = json(response).get("errors").get(0);
        assertThat(error.get("field").asString()).isEqualTo("title");
        assertThat(response.body()).doesNotContain(secretLikeValue);
    }

    @Test
    void malformedJsonIs400() {
        assertProblem(send("POST", "/test-probe/validate", userToken(), Map.of(), "{not json"), 400, "INVALID_REQUEST");
    }

    @Test
    void unknownPathIs404ProblemForAuthenticatedCaller() {
        assertProblem(get("/api/v1/does-not-exist", userToken()), 404, "RESOURCE_NOT_FOUND");
    }

    @Test
    void wrongMethodIs405WithAllowHeader() {
        HttpResponse<String> response = send("DELETE", "/test-probe/me", userToken(), Map.of(), null);
        assertProblem(response, 405, "METHOD_NOT_ALLOWED");
        assertThat(response.headers().firstValue("Allow")).isPresent();
    }

    @Test
    void managementSurfaceIsMinimal() {
        assertProblem(get("/actuator/info"), 401, "AUTHENTICATION_REQUIRED");
        assertThat(get("/actuator/info", userToken()).statusCode()).isEqualTo(200);
        for (String path : List.of("/actuator/env", "/actuator/beans", "/actuator/configprops", "/actuator/mappings")) {
            assertThat(get(path, userToken()).statusCode()).as(path).isEqualTo(404);
        }
    }

    @Test
    void apiDocsAreDisabledByDefault() {
        assertThat(get("/v3/api-docs", userToken()).statusCode()).isEqualTo(404);
        assertThat(get("/swagger-ui/index.html", userToken()).statusCode()).isEqualTo(404);
    }

    @Test
    void corsAllowsOnlyConfiguredFrontendOrigin() {
        Map<String, String> allowed = Map.of("Origin", ALLOWED_ORIGIN, "Access-Control-Request-Method", "GET");
        HttpResponse<String> ok = send("OPTIONS", "/test-probe/me", null, allowed, null);
        assertThat(ok.statusCode()).isEqualTo(200);
        assertThat(ok.headers().firstValue("Access-Control-Allow-Origin")).hasValue(ALLOWED_ORIGIN);

        Map<String, String> denied = Map.of("Origin", "https://evil.example", "Access-Control-Request-Method", "GET");
        HttpResponse<String> rejected = send("OPTIONS", "/test-probe/me", null, denied, null);
        assertThat(rejected.statusCode()).isEqualTo(403);
        assertThat(rejected.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
    }

    private static String userToken() {
        return KEYS.token(UUID.randomUUID(), "LEARNER");
    }

    private static void assertProblem(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                type -> assertThat(type).startsWith("application/problem+json"));
        JsonNode body = json(response);
        assertThat(body.get("status").asInt()).isEqualTo(status);
        assertThat(body.get("code").asString()).isEqualTo(code);
        String requestId = body.get("requestId").asString();
        assertThat(requestId).hasSize(36);
        assertThat(response.headers().firstValue("X-Request-ID")).hasValue(requestId);
    }
}
