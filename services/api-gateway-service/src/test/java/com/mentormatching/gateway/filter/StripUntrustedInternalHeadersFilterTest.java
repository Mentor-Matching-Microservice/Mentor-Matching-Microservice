package com.mentormatching.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class StripUntrustedInternalHeadersFilterTest {

    @Test
    void removesUntrustedIdentityHeadersAndPreservesOrdinaryHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "forged-user");
        request.addHeader("X-Internal-Role", "ADMIN");
        request.addHeader("X-Authenticated-User", "forged-user");
        request.addHeader("X-Request-Id", "client-request-id");
        MockFilterChain chain = new MockFilterChain();

        new StripUntrustedInternalHeadersFilter().doFilter(request, new MockHttpServletResponse(), chain);

        var forwarded = (jakarta.servlet.http.HttpServletRequest) chain.getRequest();
        assertNull(forwarded.getHeader("X-User-Id"));
        assertNull(forwarded.getHeader("X-Internal-Role"));
        assertNull(forwarded.getHeader("X-Authenticated-User"));
        assertEquals("client-request-id", forwarded.getHeader("X-Request-Id"));
        assertEquals(Collections.list(forwarded.getHeaderNames()).size(),
                Collections.list(request.getHeaderNames()).size() - 3);
    }
}
