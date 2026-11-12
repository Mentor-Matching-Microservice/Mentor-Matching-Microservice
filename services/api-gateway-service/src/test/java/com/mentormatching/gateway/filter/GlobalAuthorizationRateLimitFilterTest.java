package com.mentormatching.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class GlobalAuthorizationRateLimitFilterTest {

    private final GlobalAuthorizationRateLimitFilter filter = new GlobalAuthorizationRateLimitFilter(
            null, false, 60, Duration.ofMinutes(1), "test");

    @Test
    void rejectsMalformedBearerHeader() throws Exception {
        MockHttpServletRequest request = apiRequest();
        request.addHeader("Authorization", "Bearer not-a-jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertEquals("Bearer", response.getHeader("WWW-Authenticate"));
        assertEquals(null, chain.getRequest());
    }

    @Test
    void allowsMissingAuthorizationForDownstreamSecurityRules() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(apiRequest(), response, chain);

        assertEquals(200, response.getStatus());
        assertTrue(chain.getRequest() instanceof MockHttpServletRequest);
    }

    @Test
    void allowsCompactBearerTokenShape() throws Exception {
        MockHttpServletRequest request = apiRequest();
        request.addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.c2lnbmF0dXJl");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertTrue(chain.getRequest() != null);
    }

    private MockHttpServletRequest apiRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/users/me");
        return request;
    }
}
