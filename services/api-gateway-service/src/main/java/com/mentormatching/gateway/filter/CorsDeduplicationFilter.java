package com.mentormatching.gateway.filter;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Wraps the HttpServletResponse to prevent duplicate CORS headers from being emitted when both
 * the API Gateway and downstream microservices configure CORS.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsDeduplicationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(request, new CorsDeduplicatingResponseWrapper(response));
    }

    public static class CorsDeduplicatingResponseWrapper extends HttpServletResponseWrapper {
        private final Set<String> corsHeadersSet = new LinkedHashSet<>();

        public CorsDeduplicatingResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void addHeader(String name, String value) {
            if (isCorsHeader(name)) {
                String normalized = name.toLowerCase(Locale.ROOT);
                if (corsHeadersSet.contains(normalized) || super.containsHeader(name)) {
                    return; // Ignore duplicate CORS header
                }
                corsHeadersSet.add(normalized);
                if (value != null && value.contains(",") && "access-control-allow-origin".equals(normalized)) {
                    value = value.split(",")[0].trim();
                }
            }
            super.addHeader(name, value);
        }

        @Override
        public void setHeader(String name, String value) {
            if (isCorsHeader(name)) {
                String normalized = name.toLowerCase(Locale.ROOT);
                corsHeadersSet.add(normalized);
                if (value != null && value.contains(",") && "access-control-allow-origin".equals(normalized)) {
                    value = value.split(",")[0].trim();
                }
            }
            super.setHeader(name, value);
        }

        private boolean isCorsHeader(String name) {
            if (name == null) {
                return false;
            }
            String lower = name.toLowerCase(Locale.ROOT);
            return lower.equals("access-control-allow-origin")
                    || lower.equals("access-control-allow-credentials")
                    || lower.equals("access-control-allow-methods")
                    || lower.equals("access-control-allow-headers")
                    || lower.equals("access-control-max-age")
                    || lower.equals("access-control-expose-headers");
        }
    }
}

