package com.mentormatching.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;

import com.jayway.jsonpath.JsonPath;
import com.mentormatching.discovery.support.EurekaServerIntegrationTestSupport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Kiểm tra quyền truy cập trên server thật (đầy đủ filter chain, Jersey và actuator).
 */
class DiscoverySecurityIT extends EurekaServerIntegrationTestSupport {

    @Test
    void healthIsPublicAndDoesNotExposeDetails() {
        HttpResponse<String> response = getAnonymous("/actuator/health");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(response.body(), "$.status")).isEqualTo("UP");
        assertThat(response.body()).doesNotContain("components", "details", "diskSpace");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator/health/liveness", "/actuator/health/readiness"})
    void probesArePublic(String path) {
        HttpResponse<String> response = getAnonymous(path);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(response.body(), "$.status")).isEqualTo("UP");
    }

    @Test
    void registryApiRejectsAnonymousRequestWithBodylessBasicChallenge() {
        HttpResponse<String> response = getAnonymous("/eureka/apps");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("WWW-Authenticate"))
                .hasValueSatisfying(value -> assertThat(value).startsWith("Basic realm=\"discovery-service\""));
        // Wire protocol Eureka: không body, để Eureka client ghi nhận đúng mã 401.
        assertThat(response.body()).isEmpty();
    }

    @Test
    void managementEndpointsRejectAnonymousRequestWithProblemDetail() {
        HttpResponse<String> response = getAnonymous("/actuator/info");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("WWW-Authenticate")).isPresent();
        assertThat(response.headers().firstValue("Content-Type"))
                .hasValueSatisfying(value -> assertThat(value).startsWith("application/problem+json"));
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("AUTHENTICATION_REQUIRED");
    }

    @Test
    void registryRejectsWrongPassword() {
        HttpResponse<String> response = send("GET", "/eureka/apps", basic(USERNAME, PASSWORD + "-wrong"), null);

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void registryRejectsUnknownUser() {
        HttpResponse<String> response = send("GET", "/eureka/apps", basic("someone-else", PASSWORD), null);

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void registryAcceptsInternalAccountWithoutCreatingSession() {
        HttpResponse<String> response = getAuthenticated("/eureka/apps");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Object>read(response.body(), "$.applications")).isNotNull();
        assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
    }

    @Test
    void anonymousCannotRegisterAnInstance() {
        HttpResponse<String> response = send("POST", "/eureka/apps/ANON-SERVICE", null,
                "{\"instance\":{\"instanceId\":\"anon:1\",\"app\":\"ANON-SERVICE\"}}");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(getAuthenticated("/eureka/apps/ANON-SERVICE").statusCode()).isEqualTo(404);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator/env", "/actuator/beans", "/actuator/configprops", "/actuator/heapdump",
            "/actuator/threaddump", "/actuator/loggers", "/actuator/mappings", "/actuator/metrics"})
    void sensitiveActuatorEndpointsAreNotExposedEvenToAuthenticatedCallers(String path) {
        assertThat(getAuthenticated(path).statusCode()).isEqualTo(404);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator/env", "/actuator/info", "/eureka/status", "/", "/not-a-real-path"})
    void everyNonHealthPathRequiresAuthenticationFirst(String path) {
        // 401 trước khi định tuyến: người lạ không dò được endpoint nào tồn tại.
        assertThat(getAnonymous(path).statusCode()).isEqualTo(401);
    }

    @Test
    void dashboardIsDisabledByDefault() {
        assertThat(getAuthenticated("/").statusCode()).isEqualTo(404);
    }

    @Test
    void healthEndpointOnlyAllowsReads() {
        assertThat(send("POST", "/actuator/health", null, "{}").statusCode()).isEqualTo(401);
    }
}
