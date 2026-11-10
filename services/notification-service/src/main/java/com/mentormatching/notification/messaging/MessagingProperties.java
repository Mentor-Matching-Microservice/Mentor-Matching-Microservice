package com.mentormatching.notification.messaging;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình RabbitMQ của notification-service.
 *
 * @param enabled      tắt để chạy service không cần broker (consumer không được tạo)
 * @param exchange     topic exchange dùng chung của hệ thống ({@code mentor.events}); khai báo với cùng tham số
 *                     như booking/payment/review (durable, không auto-delete) nên không xung đột
 * @param queue        queue riêng của notification-service
 * @param routingKeys  các pattern routing key được bind vào queue
 * @param maxRetries   số lần thử lại khi lỗi tạm thời (MongoDB, mạng) trước khi chuyển sang dead-letter queue
 */
@ConfigurationProperties(prefix = "notification.messaging")
public record MessagingProperties(Boolean enabled, String exchange, String queue, List<String> routingKeys,
                                  Integer maxRetries) {

    public MessagingProperties {
        enabled = enabled == null || enabled;
        exchange = exchange == null || exchange.isBlank() ? "mentor.events" : exchange;
        queue = queue == null || queue.isBlank() ? "notification.domain-events" : queue;
        routingKeys = routingKeys == null || routingKeys.isEmpty() ? List.of("auth.#", "mentor.#") : List.copyOf(routingKeys);
        maxRetries = maxRetries == null ? 4 : maxRetries;
        if (maxRetries < 0 || maxRetries > 10) {
            throw new IllegalArgumentException("notification.messaging.max-retries must be between 0 and 10");
        }
    }

    public String deadLetterExchange() {
        return queue + ".dlx";
    }

    public String deadLetterQueue() {
        return queue + ".dead";
    }
}
