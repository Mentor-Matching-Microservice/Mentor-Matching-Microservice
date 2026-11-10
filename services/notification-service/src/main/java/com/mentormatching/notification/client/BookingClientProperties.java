package com.mentormatching.notification.client;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Kết nối tới API nội bộ có sẵn của booking-service ({@code GET /internal/v1/bookings/{id}/payment-context}).
 * notification-service chỉ GỌI API này, không sửa booking-service.
 */
@ConfigurationProperties(prefix = "notification.booking")
public record BookingClientProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {

    public BookingClientProperties {
        baseUrl = baseUrl == null || baseUrl.isBlank() ? "http://localhost:8084" : stripTrailingSlash(baseUrl.strip());
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(2) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(3) : readTimeout;
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
