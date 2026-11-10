package com.mentormatching.stats.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mentormatching.stats.config.StatsProperties;
import com.mentormatching.stats.domain.DailyStats;
import com.mentormatching.stats.domain.Metric;
import com.mentormatching.stats.domain.MetricSource;
import com.mentormatching.stats.dto.OverviewResponse;
import com.mentormatching.stats.dto.TimeseriesResponse;
import com.mentormatching.stats.exception.ApiException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Đọc bộ đếm đã tổng hợp sẵn trong {@code mm_stats}; không gọi service nào khác nên luôn trả lời nhanh.
 * Mặc định 30 ngày gần nhất (giống dự án cũ), tối đa {@value #MAX_RANGE_DAYS} ngày.
 */
@Service
public class StatsQueryService {

    public static final int DEFAULT_RANGE_DAYS = 30;
    public static final int MAX_RANGE_DAYS = 366;

    private final MongoTemplate mongo;
    private final StatsProperties properties;
    private final Clock clock;

    public StatsQueryService(MongoTemplate mongo, StatsProperties properties, Clock clock) {
        this.mongo = mongo;
        this.properties = properties;
        this.clock = clock;
    }

    public OverviewResponse overview(LocalDate from, LocalDate to) {
        Range range = resolve(from, to);
        List<DailyStats> days = load(range);
        Set<Metric> unavailable = unavailableMetrics();

        long created = sum(days, DailyStats::getBookingsCreated);
        long completed = sum(days, DailyStats::getBookingsCompleted);
        BigDecimal revenue = money(days, DailyStats::getRevenue);
        BigDecimal refunded = money(days, DailyStats::getRefunded);
        boolean rateAvailable = !unavailable.contains(Metric.BOOKINGS_CREATED)
                && !unavailable.contains(Metric.BOOKINGS_COMPLETED);
        double completionRate = rateAvailable && created > 0 ? (double) completed / created : 0.0;
        List<String> unavailableFields = overviewFields(unavailable);

        return new OverviewResponse(range.from(), range.to(), sum(days, DailyStats::getNewUsers),
                sum(days, DailyStats::getNewMentors), created, completed, completionRate, revenue,
                sum(days, DailyStats::getBookingsConfirmed), sum(days, DailyStats::getBookingsCancelled),
                sum(days, DailyStats::getBookingsRejected), sum(days, DailyStats::getPaymentsSucceeded), refunded,
                revenue.subtract(refunded), properties.currency(), !unavailableFields.isEmpty(), unavailableFields);
    }

    public TimeseriesResponse timeseries(LocalDate from, LocalDate to) {
        Range range = resolve(from, to);
        Map<LocalDate, DailyStats> byDay = load(range).stream()
                .collect(Collectors.toMap(DailyStats::getDate, Function.identity()));
        List<TimeseriesResponse.Point> points = range.from().datesUntil(range.to().plusDays(1))
                .map(date -> {
                    DailyStats day = byDay.get(date);
                    return day == null
                            ? new TimeseriesResponse.Point(date, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO)
                            : new TimeseriesResponse.Point(date, day.getBookingsCreated(), day.getBookingsConfirmed(),
                                    day.getRevenue(), day.getRefunded());
                })
                .toList();
        List<String> unavailableFields = unavailableMetrics().contains(Metric.BOOKINGS_CREATED)
                ? List.of("bookingsCount") : List.of();
        return new TimeseriesResponse(range.from(), range.to(), properties.currency(), !unavailableFields.isEmpty(),
                unavailableFields, points);
    }

    /** Chỉ số có nguồn chưa từng được nhận (và không thuộc nhóm đã publish trên develop). */
    Set<Metric> unavailableMetrics() {
        Set<String> seen = mongo.findAll(MetricSource.class).stream().map(MetricSource::getEventType)
                .collect(Collectors.toSet());
        Set<Metric> unavailable = EnumSet.noneOf(Metric.class);
        for (Metric metric : Metric.values()) {
            if (!metric.alwaysPublished() && !seen.contains(metric.eventType())) {
                unavailable.add(metric);
            }
        }
        return unavailable;
    }

    private static List<String> overviewFields(Set<Metric> unavailable) {
        List<String> fields = new ArrayList<>();
        if (unavailable.contains(Metric.NEW_USERS)) fields.add("newUsersCount");
        if (unavailable.contains(Metric.NEW_MENTORS)) fields.add("newMentorsCount");
        if (unavailable.contains(Metric.BOOKINGS_CREATED)) fields.add("totalBookings");
        if (unavailable.contains(Metric.BOOKINGS_COMPLETED)) fields.add("completedBookings");
        if (unavailable.contains(Metric.BOOKINGS_CREATED) || unavailable.contains(Metric.BOOKINGS_COMPLETED)) {
            fields.add("completionRate");
        }
        return List.copyOf(fields);
    }

    private Range resolve(LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now(clock.withZone(properties.zone()));
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_RANGE_DAYS - 1L);
        if (start.isAfter(end)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "from must not be after to.");
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_RANGE_DAYS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DATE_RANGE_TOO_LARGE",
                    "The date range must not exceed " + MAX_RANGE_DAYS + " days.");
        }
        return new Range(start, end);
    }

    private List<DailyStats> load(Range range) {
        // _id là "yyyy-MM-dd" nên so sánh chuỗi đúng thứ tự ngày.
        return mongo.find(Query.query(Criteria.where("_id").gte(range.from().toString()).lte(range.to().toString())),
                DailyStats.class);
    }

    private static long sum(List<DailyStats> days, java.util.function.ToLongFunction<DailyStats> field) {
        return days.stream().mapToLong(field).sum();
    }

    private static BigDecimal money(List<DailyStats> days, Function<DailyStats, BigDecimal> field) {
        return days.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private record Range(LocalDate from, LocalDate to) {
    }
}
