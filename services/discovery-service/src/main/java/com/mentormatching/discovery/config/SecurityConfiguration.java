package com.mentormatching.discovery.config;

import jakarta.servlet.DispatcherType;

import com.mentormatching.discovery.security.ProblemAuthenticationEntryPoint;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Bảo vệ registry: chỉ health probe là công khai. Mọi endpoint còn lại (REST API {@code /eureka/**},
 * dashboard, actuator khác) yêu cầu tài khoản nội bộ qua HTTP Basic.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    public static final String REALM = "discovery-service";

    @Bean
    ProblemAuthenticationEntryPoint problemAuthenticationEntryPoint() {
        return new ProblemAuthenticationEntryPoint(REALM);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ProblemAuthenticationEntryPoint entryPoint)
            throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        // Error dispatch chỉ xảy ra sau khi request ban đầu đã qua kiểm tra quyền.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        // Probe cho Docker/K8s; show-details=never nên không lộ thông tin nội bộ.
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().hasRole(DiscoveryCredentialsConfiguration.REGISTRY_ROLE))
                .httpBasic(basic -> basic
                        .realmName(REALM)
                        .authenticationEntryPoint(entryPoint))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(entryPoint))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Eureka client gửi POST/PUT/DELETE không có CSRF token và xác thực bằng header Basic,
                // không dùng session cookie. Request ghi từ trình duyệt khác origin là non-simple
                // (JSON/PUT/DELETE) nên bị chặn ở CORS preflight vì registry không bật CORS.
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .build();
    }
}
