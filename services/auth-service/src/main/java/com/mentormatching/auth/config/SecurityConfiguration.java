package com.mentormatching.auth.config;

import com.mentormatching.auth.controller.AccountRecoveryController;
import com.mentormatching.auth.controller.JwksController;

import com.mentormatching.auth.service.AccountUserDetailsService;
import com.mentormatching.auth.filter.OriginFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Cấu hình các thành phần bảo mật của Auth Service.
 */
@Configuration
public class SecurityConfiguration {

    /**
     * Endpoint POST công khai của luồng xác thực. Không dùng cookie phiên nên CSRF được bỏ qua; thay vào đó
     * {@link OriginFilter} chỉ cho phép trình duyệt từ origin frontend đã cấu hình.
     */
    public static final String[] PUBLIC_POST_ENDPOINTS = {
            "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh-token", "/api/v1/auth/logout",
            AccountRecoveryController.VERIFY_EMAIL, AccountRecoveryController.RESEND_VERIFICATION,
            AccountRecoveryController.FORGOT_PASSWORD, AccountRecoveryController.RESET_PASSWORD
    };
    /**
     * Tạo bộ mã hóa và kiểm tra mật khẩu bằng BCrypt.
     *
     * @return bộ mã hóa mật khẩu
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Cấu hình xác thực email và mật khẩu qua Spring Security.
     *
     * @param userDetailsService bộ tải tài khoản
     * @param passwordEncoder    bộ kiểm tra mật khẩu
     * @return bộ quản lý xác thực
     */
    @Bean
    AuthenticationManager authenticationManager(AccountUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        var provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /**
     * Cho phép các endpoint xác thực công khai và kiểm tra Origin cho cookie.
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, OriginFilter originFilter) throws Exception {
        return http
                .cors(cors -> {
                })
                .addFilterBefore(originFilter, CsrfFilter.class)
                .csrf(csrf -> csrf.ignoringRequestMatchers(PUBLIC_POST_ENDPOINTS))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, JwksController.PATH).permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .build();
    }

    /**
     * Filter chỉ chạy trong Spring Security, không đăng ký thêm ở servlet container.
     */
    @Bean
    FilterRegistrationBean<OriginFilter> authOriginFilterRegistration(OriginFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

}
