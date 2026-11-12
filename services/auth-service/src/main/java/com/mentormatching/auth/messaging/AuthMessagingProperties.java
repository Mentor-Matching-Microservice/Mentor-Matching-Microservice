package com.mentormatching.auth.messaging;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình RabbitMQ của auth-service.
 *
 * @param enabled              false: không publish và không nhận sự kiện (outbox vẫn được ghi, gửi bù khi bật lại)
 * @param exchange             topic exchange dùng chung {@code mentor.events} (cùng tham số với các service khác)
 * @param queue                queue riêng nhận sự kiện cho auth-service ({@code MentorApproved})
 * @param routingKeys          pattern được bind vào queue
 * @param maxRetries           số lần thử lại khi xử lý sự kiện gặp lỗi tạm thời
 * @param confirmTimeout       chờ broker xác nhận mỗi sự kiện outbox
 */
@ConfigurationProperties(prefix = "auth.messaging")
public record AuthMessagingProperties(Boolean enabled, String exchange, String queue, List<String> routingKeys,
                                      Integer maxRetries, Duration confirmTimeout) {

    public AuthMessagingProperties {
        enabled = enabled == null || enabled;
        exchange = exchange == null || exchange.isBlank() ? "mentor.events" : exchange;
        queue = queue == null || queue.isBlank() ? "auth.mentor-events" : queue;
        routingKeys = routingKeys == null || routingKeys.isEmpty() ? List.of("mentor.#") : List.copyOf(routingKeys);
        maxRetries = maxRetries == null ? 4 : maxRetries;
        confirmTimeout = confirmTimeout == null ? Duration.ofSeconds(5) : confirmTimeout;
        if (maxRetries < 0 || maxRetries > 10) {
            throw new IllegalArgumentException("auth.messaging.max-retries must be between 0 and 10");
        }
    }

    public String deadLetterExchange() {
        return queue + ".dlx";
    }

    public String deadLetterQueue() {
        return queue + ".dead";
    }
}
