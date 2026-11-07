package com.mentormatching.discovery.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Trả 401 kèm challenge HTTP Basic.
 * <ul>
 *   <li>REST API Eureka ({@code /eureka/**}): body rỗng, đúng wire protocol của Eureka. Nếu trả JSON,
 *   Eureka client sẽ cố đọc body thành registry, lỗi parse và log "Request execution error" thay vì ghi nhận 401.</li>
 *   <li>Các đường dẫn khác (actuator, dashboard): ProblemDetail theo RFC 9457, cùng contract lỗi với các service khác.</li>
 * </ul>
 * Body là chuỗi cố định, không phản hồi lại input của client.
 */
public class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {

    static final String EUREKA_API_PREFIX = "/eureka/";

    static final String BODY = """
            {"type":"about:blank","title":"Unauthorized","status":401,\
            "detail":"Authentication is required to access the service registry.",\
            "code":"AUTHENTICATION_REQUIRED"}""";

    private final String challenge;

    public ProblemAuthenticationEntryPoint(String realmName) {
        this.challenge = "Basic realm=\"" + realmName + "\", charset=\"UTF-8\"";
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, challenge);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        if (isEurekaApi(request)) {
            response.setContentLength(0);
            return;
        }
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(BODY);
    }

    private static boolean isEurekaApi(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.startsWith(EUREKA_API_PREFIX);
    }
}
