package com.mentormatching.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Removes client-supplied identity and internal metadata before proxying requests. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 3)
public class StripUntrustedInternalHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(new SanitizedRequest(request), response);
    }

    private static boolean isUntrusted(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return normalized.startsWith("x-internal-")
                || normalized.startsWith("x-authenticated-")
                || normalized.startsWith("x-user-");
    }

    private static final class SanitizedRequest extends HttpServletRequestWrapper {
        private SanitizedRequest(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getHeader(String name) {
            return isUntrusted(name) ? null : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return isUntrusted(name) ? Collections.emptyEnumeration() : super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> allowed = Collections.list(super.getHeaderNames()).stream()
                    .filter(name -> !isUntrusted(name)).toList();
            return Collections.enumeration(allowed);
        }
    }
}
