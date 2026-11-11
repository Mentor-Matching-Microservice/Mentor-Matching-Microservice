package com.mentormatching.discovery.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * Nạp tài khoản nội bộ của registry từ môi trường.
 * Ứng dụng không khởi động nếu thiếu hoặc sai {@code DISCOVERY_USERNAME}/{@code DISCOVERY_PASSWORD}.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DiscoverySecurityProperties.class)
public class DiscoveryCredentialsConfiguration {

    /** Role duy nhất của registry; người vận hành và service client dùng chung tài khoản nội bộ. */
    public static final String REGISTRY_ROLE = "DISCOVERY_CLIENT";

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * Mật khẩu chỉ được giữ ở dạng BCrypt trong bộ nhớ.
     * Khai báo bean này cũng tắt user mặc định của Spring Boot, nên log không còn "generated security password".
     */
    @Bean
    UserDetailsService discoveryUserDetailsService(DiscoverySecurityProperties properties, PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(User.withUsername(properties.username())
                .password(encoder.encode(properties.password()))
                .roles(REGISTRY_ROLE)
                .build());
    }
}
