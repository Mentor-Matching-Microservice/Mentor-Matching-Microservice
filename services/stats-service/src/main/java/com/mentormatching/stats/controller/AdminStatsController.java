package com.mentormatching.stats.controller;

import java.time.LocalDate;

import com.mentormatching.stats.dto.ApiResponse;
import com.mentormatching.stats.dto.OverviewResponse;
import com.mentormatching.stats.dto.TimeseriesResponse;
import com.mentormatching.stats.service.StatsQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thống kê cho trang Admin Dashboard ({@code /admin}) và Reports ({@code /admin/reports}). Chỉ ROLE_ADMIN.
 * {@code from}/{@code to} dạng {@code yyyy-MM-dd}, tính theo ngày giờ Việt Nam; mặc định 30 ngày gần nhất.
 */
@RestController
@RequestMapping("/api/v1/admin/stats")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsController {

    private final StatsQueryService stats;

    public AdminStatsController(StatsQueryService stats) {
        this.stats = stats;
    }

    @GetMapping("/overview")
    public ApiResponse<OverviewResponse> overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok("Get admin stats overview successfully", stats.overview(from, to));
    }

    @GetMapping("/timeseries")
    public ApiResponse<TimeseriesResponse> timeseries(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok("Get admin stats timeseries successfully", stats.timeseries(from, to));
    }
}
