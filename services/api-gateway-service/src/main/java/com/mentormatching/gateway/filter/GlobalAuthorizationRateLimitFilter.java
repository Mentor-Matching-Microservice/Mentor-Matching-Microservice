package com.mentormatching.gateway.filter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/** Applies a lightweight Bearer JWT shape check and a shared Redis request limit to API traffic. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class GlobalAuthorizationRateLimitFilter extends OncePerRequestFilter {

    private static final Pattern COMPACT_JWT = Pattern.compile(
            "^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$");
    private static final String RATE_LIMIT_SCRIPT = "local count = redis.call('INCR', KEYS[1]); "
            + "if count == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]); end; "
            + "return count;";

    private final StringRedisTemplate redisTemplate;
    private final boolean rateLimitEnabled;
    private final long requestLimit;
    private final Duration rateLimitWindow;
    private final String keyPrefix;
    private final DefaultRedisScript<Long> rateLimitScript;

    public GlobalAuthorizationRateLimitFilter(
            StringRedisTemplate redisTemplate,
            @Value("${gateway.rate-limit.enabled:true}") boolean rateLimitEnabled,
            @Value("${gateway.rate-limit.requests:60}") long requestLimit,
            @Value("${gateway.rate-limit.window:PT1M}") Duration rateLimitWindow,
            @Value("${gateway.rate-limit.key-prefix:mentor-gateway:rate-limit}") String keyPrefix) {
        this.redisTemplate = redisTemplate;
        this.rateLimitEnabled = rateLimitEnabled;
        this.requestLimit = requestLimit;
        this.rateLimitWindow = rateLimitWindow;
        this.keyPrefix = keyPrefix;
        this.rateLimitScript = new DefaultRedisScript<>(RATE_LIMIT_SCRIPT, Long.class);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (rateLimitEnabled && !checkRateLimit(request, response)) {
            return;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization) && !hasValidBearerShape(authorization)) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Authorization must contain a compact Bearer JWT.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean checkRateLimit(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String remoteAddress = request.getRemoteAddr();
        String key = keyPrefix + ":" + (StringUtils.hasText(remoteAddress) ? remoteAddress : "unknown");
        try {
            Long count = redisTemplate.execute(rateLimitScript, List.of(key),
                    Long.toString(rateLimitWindow.toMillis()));
            if (count == null) {
                writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                        "Rate limit service is unavailable.");
                return false;
            }
            if (count > requestLimit) {
                response.setHeader("Retry-After", Long.toString(Math.max(1, rateLimitWindow.toSeconds())));
                writeError(response, 429,
                        "Request limit exceeded. Try again later.");
                return false;
            }
            return true;
        } catch (RuntimeException exception) {
            writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Rate limit service is unavailable.");
            return false;
        }
    }

    private boolean hasValidBearerShape(String authorization) {
        int separator = authorization.indexOf(' ');
        if (separator < 0 || !"bearer".equals(authorization.substring(0, separator).toLowerCase(Locale.ROOT))) {
            return false;
        }
        return COMPACT_JWT.matcher(authorization.substring(separator + 1).trim()).matches();
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"status\":%d,\"error\":\"%s\"}".formatted(status, message));
    }
}
