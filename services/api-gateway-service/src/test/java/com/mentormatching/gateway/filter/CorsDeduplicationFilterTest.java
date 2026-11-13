package com.mentormatching.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorsDeduplicationFilterTest {

    @Test
    void deduplicatesAccessControlAllowOriginHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                var httpRes = (jakarta.servlet.http.HttpServletResponse) res;
                httpRes.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
                httpRes.addHeader("Access-Control-Allow-Origin", "http://localhost:5173");
                httpRes.addHeader("Access-Control-Allow-Credentials", "true");
                httpRes.addHeader("Access-Control-Allow-Credentials", "true");
            }
        };

        new CorsDeduplicationFilter().doFilter(request, response, chain);

        assertEquals(1, response.getHeaders("Access-Control-Allow-Origin").size());
        assertEquals("http://localhost:5173", response.getHeader("Access-Control-Allow-Origin"));
        assertEquals(1, response.getHeaders("Access-Control-Allow-Credentials").size());
        assertEquals("true", response.getHeader("Access-Control-Allow-Credentials"));
    }
}
