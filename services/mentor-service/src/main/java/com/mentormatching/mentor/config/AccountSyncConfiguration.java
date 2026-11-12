package com.mentormatching.mentor.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

import com.mentormatching.mentor.client.AuthAccountStatusClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

/**
 * Cấu hình HTTP client và bật scheduling khi mentor.account-sync.enabled=true.
 * Client được tạo một lần, dùng token nội bộ để gọi auth-service; không dùng JWT người dùng.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "mentor.account-sync.enabled", havingValue = "true")
public class AccountSyncConfiguration {

    /**
     * Tạo client với timeout kết nối 3 giây và chờ response 5 giây.
     *
     * @param baseUrl địa chỉ HTTP/HTTPS của auth-service, phải có host và không chứa user info
     * @param token bí mật dùng chung với auth, tối thiểu 32 ký tự; không phải public key
     * @return client dùng để lấy trạng thái tài khoản theo batch
     * @throws IllegalArgumentException nếu URL hoặc token không hợp lệ
     */
    @Bean
    AuthAccountStatusClient authAccountStatusClient(@Value("${mentor.account-sync.base-url}") String baseUrl,
                                                    @Value("${mentor.account-sync.token}") String token) {

        // Tạo instance URI từ url auth service
        URI uri = URI.create(baseUrl);
        if (uri.getHost() == null || uri.getUserInfo() != null
                || !("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
                || token.isBlank() || token.length() < 32)
            throw new IllegalArgumentException("Invalid account sync configuration");

        var factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .build()
        );
        factory.setReadTimeout(Duration.ofSeconds(5));

        return new AuthAccountStatusClient(RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("Authorization", "Bearer " + token)
                .build());
    }
}
