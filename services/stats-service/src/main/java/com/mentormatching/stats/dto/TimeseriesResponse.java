package com.mentormatching.stats.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Chuỗi theo ngày cho biểu đồ Recharts: mỗi ngày trong [from, to] đều có một điểm (ngày không phát sinh = 0).
 * {@code date}, {@code bookingsCount}, {@code revenue} giữ tên của dự án cũ.
 */
public record TimeseriesResponse(LocalDate from, LocalDate to, String currency, boolean partialData,
                                 List<String> unavailableMetrics, List<Point> points) {

    public record Point(LocalDate date, long bookingsCount, long confirmedBookings, BigDecimal revenue,
                        BigDecimal refunded) {
    }
}
