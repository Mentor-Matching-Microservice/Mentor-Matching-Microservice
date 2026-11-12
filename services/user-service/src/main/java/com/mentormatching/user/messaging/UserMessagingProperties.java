package com.mentormatching.user.messaging;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình RabbitMQ của user-service.
 *
 * @param enabled     false: không nhận sự kiện (consumer không được tạo)
 * @param exchange    topic exchange dùng chung {@code mentor.events} (cùng tham số với các service khác)
 * @param queue       queue riêng của user-service
 * @param routingKeys pattern được bind vào queue
 * @param maxRetries  số lần thử lại khi lỗi tạm thời trước khi vào dead-letter queue
 */
@ConfigurationProperties(prefix = "user.messaging")
public record UserMessagingProperties(Boolean enabled, String exchange, String queue, List<String> routingKeys,
                                      Integer maxRetries) {

    public UserMessagingProperties {
        enabled = enabled == null || enabled;
        exchange = exchange == null || exchange.isBlank() ? "mentor.events" : exchange;
        queue = queue == null || queue.isBlank() ? "user.account-events" : queue;
        routingKeys = routingKeys == null || routingKeys.isEmpty() ? List.of("auth.account.registered") : List.copyOf(routingKeys);
        maxRetries = maxRetries == null ? 4 : maxRetries;
        if (maxRetries < 0 || maxRetries > 10) {
            throw new IllegalArgumentException("user.messaging.max-retries must be between 0 and 10");
        }
    }

    public String deadLetterExchange() {
        return queue + ".dlx";
    }

    public String deadLetterQueue() {
        return queue + ".dead";
    }
}
