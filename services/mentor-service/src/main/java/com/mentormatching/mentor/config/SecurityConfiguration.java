package com.mentormatching.mentor.config;

import com.mentormatching.mentor.security.ProblemAuthenticationEntryPoint;
import com.mentormatching.mentor.security.ProblemAccessDeniedHandler;

import jakarta.servlet.DispatcherType;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Bảo vệ API bằng Bearer JWT, không sử dụng session hoặc HTTP Basic.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    /**
     * Chuyển claim roles thành authorities với tiền tố ROLE_.
     */
    @Bean
    JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter() {
        var converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthoritiesClaimName("roles");
        converter.setAuthorityPrefix("ROLE_");
        return converter;
    }

    /**
     * Xác thực JWT và trả lỗi bảo mật theo ProblemDetail.
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ProblemAuthenticationEntryPoint entryPoint,
                                            ProblemAccessDeniedHandler deniedHandler, CorsConfigurationSource corsConfigurationSource,
                                            JwtGrantedAuthoritiesConverter authorities) throws Exception {
        var authentication = new JwtAuthenticationConverter();
        authentication.setJwtGrantedAuthoritiesConverter(authorities);
        return http
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/catalog/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/locations/**").permitAll()
                        .requestMatchers("/internal/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/mentors",
                                "/api/v1/mentors/personality-options",
                                "/api/v1/mentors/highlight-options",
                                "/api/v1/mentors/{mentorId:[0-9]+}",
                                "/api/v1/mentors/{mentorId:[0-9]+}/subjects",
                                "/api/v1/mentors/{mentorId:[0-9]+}/traits",
                                "/api/v1/mentors/{mentorId:[0-9]+}/achievements").permitAll()
                        .anyRequest().authenticated())

                // Exception Handling
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(entryPoint) // Entry point
                        .accessDeniedHandler(deniedHandler)) // Access Denied Handler

                // Oauth2 Resource Server
                .oauth2ResourceServer(resource -> resource
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(authentication))
                        .authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler))

                // STATELESS Session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // API chỉ nhận token qua Authorization header, không xác thực bằng cookie.
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .build();
    }

}
