package com.mentormatching.auth.config;

import com.mentormatching.auth.controller.AccountRecoveryController;
import com.mentormatching.auth.controller.JwksController;

import com.mentormatching.auth.exception.ProblemResponses;
import jakarta.servlet.DispatcherType;
import com.mentormatching.auth.service.AccountUserDetailsService;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
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
@EnableMethodSecurity
public class SecurityConfiguration {

    /**
     * Endpoint POST công khai của luồng xác thực. Không dùng cookie phiên nên CSRF được bỏ qua; thay vào đó
     * {@link OriginFilter} chỉ cho phép trình duyệt từ origin frontend đã cấu hình.
     */
    /** API quản trị (#64), chỉ ROLE_ADMIN. */
    public static final String ADMIN_API = "/api/v1/admin/**";

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
    SecurityFilterChain securityFilterChain(HttpSecurity http, OriginFilter originFilter, ProblemResponses problems,
                                            JwtDecoder accessTokenDecoder) throws Exception {
        var roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        var authentication = new JwtAuthenticationConverter();
        authentication.setJwtGrantedAuthoritiesConverter(roles);
        return http
                .cors(cors -> {
                })
                .addFilterBefore(originFilter, CsrfFilter.class)
                // API admin xác thực bằng header Bearer (không cookie) nên không cần CSRF.
                .csrf(csrf -> csrf.ignoringRequestMatchers(PUBLIC_POST_ENDPOINTS)
                        .ignoringRequestMatchers(ADMIN_API))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Trang lỗi nội bộ (vd. 403 của OriginFilter) giữ nguyên status gốc, không bị đổi thành 401.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, JwksController.PATH).permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(ADMIN_API).hasRole("ADMIN")
                        .anyRequest().authenticated())
                // #64: API admin dùng access token do chính auth-service ký (kiểm tra chữ ký, iss, aud, exp).
                .oauth2ResourceServer(resource -> resource
                        .jwt(jwt -> jwt.decoder(accessTokenDecoder).jwtAuthenticationConverter(authentication))
                        .authenticationEntryPoint((req, res, ex) -> {
                            res.setHeader("WWW-Authenticate", "Bearer");
                            problems.write(HttpStatus.UNAUTHORIZED, req, res);
                        })
                        .accessDeniedHandler((req, res, ex) -> problems.write(HttpStatus.FORBIDDEN, req, res)))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((req, res, ex) -> {
                            res.setHeader("WWW-Authenticate", "Bearer");
                            problems.write(HttpStatus.UNAUTHORIZED, req, res);
                        })
                        .accessDeniedHandler((req, res, ex) -> problems.write(HttpStatus.FORBIDDEN, req, res)))
                .build();
    }

    /** Kiểm tra access token của chính auth-service: chữ ký RS256 bằng public key của khóa ký, issuer và audience. */
    @Bean
    JwtDecoder accessTokenDecoder(RSAKey jwtSigningKey, @Value("${auth.jwt.issuer}") String issuer,
                                  @Value("${auth.jwt.audience}") String audience) throws Exception {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(jwtSigningKey.toRSAPublicKey())
                .signatureAlgorithm(SignatureAlgorithm.RS256).build();
        OAuth2TokenValidator<Jwt> audienceCheck = jwt -> jwt.getAudience() != null && jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid audience", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), audienceCheck));
        return decoder;
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
