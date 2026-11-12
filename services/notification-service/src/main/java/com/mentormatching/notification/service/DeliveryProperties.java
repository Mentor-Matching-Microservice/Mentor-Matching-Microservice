package com.mentormatching.notification.service;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Retry khi đẩy realtime gặp lỗi tạm thời.
 *
 * @param maxAttempts    tổng số lần thử cho mỗi thông báo (1–5)
 * @param initialBackoff chờ trước lần thử thứ 2; nhân đôi ở mỗi lần sau
 */
@ConfigurationProperties(prefix = "notification.delivery")
public record DeliveryProperties(Integer maxAttempts, Duration initialBackoff) {

    public DeliveryProperties {
        maxAttempts = maxAttempts == null ? 3 : maxAttempts;
        initialBackoff = initialBackoff == null ? Duration.ofMillis(200) : initialBackoff;
        if (maxAttempts < 1 || maxAttempts > 5) {
            throw new IllegalArgumentException("notification.delivery.max-attempts must be between 1 and 5");
        }
        if (initialBackoff.isNegative() || initialBackoff.compareTo(Duration.ofSeconds(5)) > 0) {
            throw new IllegalArgumentException("notification.delivery.initial-backoff must be between 0 and 5s");
        }
    }
}
