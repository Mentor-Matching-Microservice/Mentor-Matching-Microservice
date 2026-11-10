package com.mentormatching.stats.config;

import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param zone     múi giờ để chia sự kiện theo ngày (mặc định giờ Việt Nam)
 * @param currency chỉ cộng doanh thu/hoàn tiền của đơn vị tiền này (mặc định VND); thanh toán tiền khác vẫn được đếm
 */
@ConfigurationProperties(prefix = "stats")
public record StatsProperties(ZoneId zone, String currency) {

    public StatsProperties {
        zone = zone == null ? ZoneId.of("Asia/Ho_Chi_Minh") : zone;
        currency = currency == null || currency.isBlank() ? "VND" : currency.strip().toUpperCase(java.util.Locale.ROOT);
    }
}
