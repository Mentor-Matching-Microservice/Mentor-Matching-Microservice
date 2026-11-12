package com.mentormatching.auth.config;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import com.mentormatching.auth.exception.ProblemResponses;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
/**
 * Bảo vệ /internal/accounts/** bằng token bí mật dùng chung, độc lập JWT người dùng.
 * Chỉ tạo security chain này khi auth.internal.account-sync.enabled=true.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
@ConditionalOnProperty(name = "auth.internal.account-sync.enabled", havingValue = "true")
public class InternalAccountSecurityConfiguration {
    /**
     * Tạo stateless security chain ưu tiên trước chain mặc định cho các đường dẫn nội bộ.
     * So sánh Bearer token bằng MessageDigest.isEqual, cấp quyền đọc account status và cấp role
     * MENTOR qua các endpoint nội bộ đã khai báo; dọn SecurityContext sau xử lý.
     *
     * @param http builder cấu hình security chain
     * @param problems helper trả ProblemDetail 401/403 an toàn
     * @param token bí mật nội bộ ít nhất 32 ký tự, không phải JWT đăng nhập
     * @return security chain dành riêng cho các account API nội bộ
     * @throws IllegalArgumentException nếu token trống hoặc quá ngắn
     * @throws Exception nếu Spring Security không xây dựng được chain
     */
    @Bean @Order(1)
    SecurityFilterChain internalAccountChain(HttpSecurity http, ProblemResponses problems,
            @Value("${auth.internal.account-sync.token}") String token) throws Exception {
        if (token.isBlank() || token.length() < 32) throw new IllegalArgumentException("Internal sync token requires at least 32 characters");
        byte[] expected = ("Bearer " + token).getBytes(StandardCharsets.UTF_8);
        var filter = new OncePerRequestFilter() {
            /**
             * Kiểm tra token trước khi chuyển tiếp request và không lưu token trong Authentication.
             * @param request request HTTP hiện tại
             * @param response response HTTP hiện tại
             * @param chain chuỗi filter tiếp theo
             * @throws ServletException nếu filter tiếp theo thất bại ở servlet
             * @throws IOException nếu ghi response hoặc xử lý I/O thất bại
             */
            @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                    FilterChain chain) throws ServletException, IOException {
                String supplied = request.getHeader("Authorization");
                if (supplied == null || !MessageDigest.isEqual(expected, supplied.getBytes(StandardCharsets.UTF_8))) {
                    response.setHeader("WWW-Authenticate", "Bearer");
                    problems.write(HttpStatus.UNAUTHORIZED, request, response); return;
                }
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("mentor-account-sync", null,
                        List.of(new SimpleGrantedAuthority("ACCOUNT_STATUS_READ"),
                                new SimpleGrantedAuthority("MENTOR_ROLE_GRANT"))));
                SecurityContextHolder.setContext(context);
                try { chain.doFilter(request, response); } finally { SecurityContextHolder.clearContext(); }
            }
        };
        return http.securityMatcher("/internal/accounts/**")
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(AbstractHttpConfigurer::disable).cors(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable).formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable).requestCache(AbstractHttpConfigurer::disable)
            .addFilterBefore(filter, AnonymousAuthenticationFilter.class)
            .authorizeHttpRequests(a -> a.requestMatchers(org.springframework.http.HttpMethod.POST, "/internal/accounts/statuses")
                    .hasAuthority("ACCOUNT_STATUS_READ")
                    .requestMatchers(org.springframework.http.HttpMethod.PUT, "/internal/accounts/*/mentor-role")
                    .hasAuthority("MENTOR_ROLE_GRANT").anyRequest().denyAll())
            .exceptionHandling(e -> e.accessDeniedHandler((req, res, ex) -> problems.write(HttpStatus.FORBIDDEN, req, res)))
            .build();
    }
}
