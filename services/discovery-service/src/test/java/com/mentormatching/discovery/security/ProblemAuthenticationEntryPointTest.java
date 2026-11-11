package com.mentormatching.discovery.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

class ProblemAuthenticationEntryPointTest {

    private final ProblemAuthenticationEntryPoint entryPoint = new ProblemAuthenticationEntryPoint("discovery-service");

    @Test
    void nonEurekaPathsGetProblemDetailWithBasicChallenge() throws Exception {
        var response = commence("/actuator/info");

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("WWW-Authenticate"))
                .isEqualTo("Basic realm=\"discovery-service\", charset=\"UTF-8\"");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(response.getContentAsString())
                .contains("\"status\":401", "\"code\":\"AUTHENTICATION_REQUIRED\"")
                // Không phản hồi lại chi tiết exception hay input của client.
                .doesNotContain("Bad credentials", "/actuator/info");
    }

    @Test
    void eurekaApiGetsBodylessChallengeSoEurekaClientsSeeAPlain401() throws Exception {
        var response = commence("/eureka/apps/USER-SERVICE");

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("WWW-Authenticate"))
                .isEqualTo("Basic realm=\"discovery-service\", charset=\"UTF-8\"");
        assertThat(response.getContentAsString()).isEmpty();
        assertThat(response.getContentType()).isNull();
    }

    @Test
    void eurekaApiDetectionHonoursContextPath() throws Exception {
        var request = new MockHttpServletRequest("GET", "/registry/eureka/apps");
        request.setContextPath("/registry");
        var response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("Bad credentials"));

        assertThat(response.getContentAsString()).isEmpty();
    }

    @Test
    void pathMerelyStartingWithEurekaIsNotTreatedAsEurekaApi() throws Exception {
        assertThat(commence("/eurekaX/apps").getContentAsString()).contains("AUTHENTICATION_REQUIRED");
    }

    private MockHttpServletResponse commence(String uri) throws Exception {
        var request = new MockHttpServletRequest("GET", uri);
        var response = new MockHttpServletResponse();
        entryPoint.commence(request, response, new BadCredentialsException("Bad credentials"));
        return response;
    }
}
