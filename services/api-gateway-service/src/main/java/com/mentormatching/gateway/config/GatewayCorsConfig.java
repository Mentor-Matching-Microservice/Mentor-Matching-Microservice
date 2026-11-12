package com.mentormatching.gateway.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class GatewayCorsConfig {

    @Bean
    FilterRegistrationBean<CorsFilter> gatewayCorsFilter(
            @Value("${gateway.cors.allowed-origins}") String allowedOrigins) {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).toList());
        cors.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type", "X-Request-Id", "X-Correlation-Id", "Idempotency-Key", "Accept", "Origin", "X-Requested-With"));
        cors.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        var registration = new FilterRegistrationBean<>(new CorsFilter(source));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
