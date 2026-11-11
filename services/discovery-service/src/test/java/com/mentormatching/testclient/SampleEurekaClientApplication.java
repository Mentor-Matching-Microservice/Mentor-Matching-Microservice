package com.mentormatching.testclient;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * Ứng dụng client tối giản chỉ dùng trong integration test, đóng vai một business service
 * (auth-service, user-service, ...) đăng ký vào registry.
 * <p>
 * Đặt ngoài package {@code com.mentormatching.discovery} để component scan của registry không quét tới.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class SampleEurekaClientApplication {
}
