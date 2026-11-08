package com.mentormatching.user.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** Cấu hình CORS cho các API của user-service. */
@Configuration(proxyBeanMethods = false)
public class CorsConfig {
    /** Cho phép frontend gửi Authorization header từ các origin đã cấu hình. */
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${user.cors.allowed-origins:http://localhost:5173}") List<String> origins) {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(origins);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        cors.setExposedHeaders(List.of("X-Request-ID", "WWW-Authenticate"));
        cors.setAllowCredentials(false);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
