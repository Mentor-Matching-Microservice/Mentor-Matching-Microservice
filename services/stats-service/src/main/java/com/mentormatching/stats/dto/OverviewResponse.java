package com.mentormatching.stats.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Tổng quan cho Admin Dashboard. Tám field đầu giữ nguyên tên của dự án cũ để frontend dùng lại.
 * Chỉ số chưa có nguồn dữ liệu trả 0 và được liệt kê trong {@code unavailableMetrics} ({@code partialData = true}).
 */
public record OverviewResponse(
        LocalDate from,
        LocalDate to,
        long newUsersCount,
        long newMentorsCount,
        long totalBookings,
        long completedBookings,
        double completionRate,
        BigDecimal totalRevenue,
        long confirmedBookings,
        long cancelledBookings,
        long rejectedBookings,
        long successfulPayments,
        BigDecimal totalRefunded,
        BigDecimal netRevenue,
        String currency,
        boolean partialData,
        List<String> unavailableMetrics) {
}
