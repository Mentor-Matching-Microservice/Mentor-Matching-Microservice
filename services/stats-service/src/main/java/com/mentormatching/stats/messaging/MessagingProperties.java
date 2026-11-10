package com.mentormatching.stats.messaging;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình RabbitMQ của stats-service.
 *
 * @param enabled      tắt để chạy service không cần broker (consumer không được tạo)
 * @param exchange     topic exchange dùng chung của hệ thống ({@code mentor.events}); khai báo với cùng tham số
 *                     như booking/payment/review (durable, không auto-delete) nên không xung đột
 * @param queue        queue riêng của stats-service
 * @param routingKeys  các pattern routing key được bind vào queue
 * @param maxRetries   số lần thử lại khi lỗi tạm thời trước khi chuyển sang dead-letter queue
 */
@ConfigurationProperties(prefix = "stats.messaging")
public record MessagingProperties(Boolean enabled, String exchange, String queue, List<String> routingKeys,
                                  Integer maxRetries) {

    /** booking/payment (đang publish) và auth/mentor (khi các service đó publish). */
    public static final List<String> DEFAULT_ROUTING_KEYS = List.of("booking.#", "payment.#", "auth.#", "mentor.#");

    public MessagingProperties {
        enabled = enabled == null || enabled;
        exchange = exchange == null || exchange.isBlank() ? "mentor.events" : exchange;
        queue = queue == null || queue.isBlank() ? "stats.domain-events" : queue;
        routingKeys = routingKeys == null || routingKeys.isEmpty() ? DEFAULT_ROUTING_KEYS : List.copyOf(routingKeys);
        maxRetries = maxRetries == null ? 4 : maxRetries;
        if (maxRetries < 0 || maxRetries > 10) {
            throw new IllegalArgumentException("stats.messaging.max-retries must be between 0 and 10");
        }
    }

    public String deadLetterExchange() {
        return queue + ".dlx";
    }

    public String deadLetterQueue() {
        return queue + ".dead";
    }
}
