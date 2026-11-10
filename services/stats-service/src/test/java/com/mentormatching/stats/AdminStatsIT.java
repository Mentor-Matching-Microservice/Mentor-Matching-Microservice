package com.mentormatching.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.mentormatching.stats.event.DomainEvent;
import com.mentormatching.stats.service.StatsIngestionService;
import com.mentormatching.stats.support.StatsIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * #82 trên RabbitMQ + MongoDB thật: sự kiện đúng định dạng booking/payment đang publish → bộ đếm theo ngày
 * (giờ Việt Nam) → overview/timeseries cho Admin Dashboard.
 */
class AdminStatsIT extends StatsIntegrationTestSupport {

    private static final String EXCHANGE = "mentor.events";
    private static final String QUEUE = "stats.domain-events";
    private static final String DEAD_LETTER_QUEUE = "stats.domain-events.dead";
    private static final String RANGE = "?from=2026-11-01&to=2026-11-10";

    @Autowired private RabbitTemplate rabbit;
    @Autowired private RabbitAdmin rabbitAdmin;
    @Autowired private MongoTemplate mongo;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private StatsIngestionService ingestion;

    private final String adminToken = KEYS.token(UUID.randomUUID(), "ADMIN");

    @BeforeEach
    void clean() {
        for (String collection : new String[] {"daily_stats", "processed_events", "metric_sources", "payment_refund_totals"}) {
            mongo.remove(new org.springframework.data.mongodb.core.query.Query(), collection);
        }
        rabbitAdmin.purgeQueue(DEAD_LETTER_QUEUE, false);
    }

    @Test
    void aggregatesPaymentsAndBookingsIntoOverview() {
        payment("PaymentSucceeded", "p1", "2026-11-05T03:00:00Z", 500000, "VND", 0);
        payment("PaymentSucceeded", "p2", "2026-11-06T03:00:00Z", 20, "USD", 0);     // đếm, không cộng doanh thu
        payment("PaymentRefunded", "p1", "2026-11-07T03:00:00Z", 500000, "VND", 200000);
        payment("PaymentRefunded", "p1", "2026-11-08T03:00:00Z", 500000, "VND", 300000); // tổng 300k → +100k
        payment("PaymentRefunded", "p1", "2026-11-09T03:00:00Z", 500000, "VND", 200000); // tới muộn → +0
        booking("BookingConfirmed", "2026-11-05T04:00:00Z");
        booking("BookingConfirmed", "2026-11-06T04:00:00Z");
        booking("BookingCancelled", "2026-11-06T05:00:00Z");
        awaitProcessed(8);

        JsonNode overview = data(get("/api/v1/admin/stats/overview" + RANGE, adminToken));

        assertThat(overview.get("from").asString()).isEqualTo("2026-11-01");
        assertThat(overview.get("totalRevenue").decimalValue()).isEqualByComparingTo("500000");
        assertThat(overview.get("totalRefunded").decimalValue()).isEqualByComparingTo("300000");
        assertThat(overview.get("netRevenue").decimalValue()).isEqualByComparingTo("200000");
        assertThat(overview.get("successfulPayments").asLong()).isEqualTo(2);
        assertThat(overview.get("confirmedBookings").asLong()).isEqualTo(2);
        assertThat(overview.get("cancelledBookings").asLong()).isEqualTo(1);
        assertThat(overview.get("currency").asString()).isEqualTo("VND");
        assertThat(overview.get("partialData").asBoolean()).isTrue();
        assertThat(overview.get("unavailableMetrics").toString())
                .contains("newUsersCount", "newMentorsCount", "totalBookings", "completedBookings", "completionRate");
    }

    @Test
    void metricBecomesAvailableOnceItsSourceStartsPublishing() {
        publish("auth.account", event("AccountRegistered", "2026-11-03T01:00:00Z",
                Map.of("accountId", UUID.randomUUID().toString())));
        awaitProcessed(1);

        JsonNode overview = data(get("/api/v1/admin/stats/overview" + RANGE, adminToken));

        assertThat(overview.get("newUsersCount").asLong()).isEqualTo(1);
        assertThat(overview.get("unavailableMetrics").toString()).doesNotContain("newUsersCount")
                .contains("newMentorsCount");
    }

    @Test
    void daysFollowVietnamTimeAndTimeseriesIsZeroFilled() {
        // 18:30 UTC ngày 05 = 01:30 ngày 06 giờ Việt Nam.
        payment("PaymentSucceeded", "p3", "2026-11-05T18:30:00Z", 150000, "VND", 0);
        awaitProcessed(1);

        JsonNode series = data(get("/api/v1/admin/stats/timeseries" + RANGE, adminToken));
        JsonNode points = series.get("points");

        assertThat(points).hasSize(10);
        assertThat(points.get(0).get("date").asString()).isEqualTo("2026-11-01");
        assertThat(points.get(9).get("date").asString()).isEqualTo("2026-11-10");
        assertThat(points.get(4).get("revenue").decimalValue()).as("Nov 5").isEqualByComparingTo("0");
        assertThat(points.get(5).get("revenue").decimalValue()).as("Nov 6").isEqualByComparingTo("150000");
        assertThat(points.get(2).get("bookingsCount").asLong()).isZero();
        assertThat(series.get("partialData").asBoolean()).isTrue();
    }

    @Test
    void emptyPeriodReturnsStableZeros() {
        JsonNode overview = data(get("/api/v1/admin/stats/overview?from=2025-01-01&to=2025-01-31", adminToken));
        assertThat(overview.get("totalRevenue").decimalValue()).isEqualByComparingTo("0");
        assertThat(overview.get("completionRate").asDouble()).isZero();

        JsonNode points = data(get("/api/v1/admin/stats/timeseries?from=2025-01-01&to=2025-01-31", adminToken))
                .get("points");
        assertThat(points).hasSize(31);
        points.forEach(p -> assertThat(p.get("revenue").decimalValue()).isEqualByComparingTo("0"));
    }

    @Test
    void redeliveredEventsAreCountedOnce() {
        byte[] body = event("BookingConfirmed", "2026-11-04T04:00:00Z", Map.of("bookingId", "7"));
        publish("booking.lifecycle", body);
        publish("booking.lifecycle", body);
        publish("booking.lifecycle", body);

        await().pollDelay(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(10)).until(() -> queueDepth(QUEUE) == 0);
        JsonNode overview = data(get("/api/v1/admin/stats/overview" + RANGE, adminToken));
        assertThat(overview.get("confirmedBookings").asLong()).isEqualTo(1);
    }

    @Test
    void concurrentDeliveriesOfOneEventAreCountedOnce() throws Exception {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("paymentId", "p-concurrent");
        fields.put("payerUserId", UUID.randomUUID().toString());
        fields.put("amount", 100000);
        fields.put("currency", "VND");
        DomainEvent event = DomainEvent.parse(event("PaymentSucceeded", "2026-11-02T03:00:00Z", fields), objectMapper);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Callable<StatsIngestionService.Outcome>> tasks = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                tasks.add(() -> ingestion.ingest(event));
            }
            List<StatsIngestionService.Outcome> outcomes = new ArrayList<>();
            for (Future<StatsIngestionService.Outcome> f : pool.invokeAll(tasks)) {
                outcomes.add(f.get(30, TimeUnit.SECONDS));
            }
            assertThat(outcomes).containsOnlyOnce(StatsIngestionService.Outcome.COUNTED);
        } finally {
            pool.shutdownNow();
        }
        JsonNode overview = data(get("/api/v1/admin/stats/overview" + RANGE, adminToken));
        assertThat(overview.get("totalRevenue").decimalValue()).isEqualByComparingTo("100000");
        assertThat(overview.get("successfulPayments").asLong()).isEqualTo(1);
    }

    @Test
    void malformedEventsGoToTheDeadLetterQueue() {
        publish("payment.events", "{oops".getBytes(StandardCharsets.UTF_8));
        await().atMost(Duration.ofSeconds(10)).until(() -> queueDepth(DEAD_LETTER_QUEUE) == 1);
    }

    @Test
    void dateRangeIsValidated() {
        assertProblem(get("/api/v1/admin/stats/overview?from=2026-11-10&to=2026-11-01", adminToken), 400,
                "INVALID_DATE_RANGE");
        assertProblem(get("/api/v1/admin/stats/timeseries?from=2024-01-01&to=2026-01-01", adminToken), 400,
                "DATE_RANGE_TOO_LARGE");
        assertThat(get("/api/v1/admin/stats/overview?from=10-11-2026", adminToken).statusCode()).isEqualTo(400);
        HttpResponse<String> defaults = get("/api/v1/admin/stats/timeseries", adminToken);
        assertThat(defaults.statusCode()).isEqualTo(200);
        assertThat(data(defaults).get("points")).hasSize(30);
    }

    @Test
    void onlyAdminsCanReadStatistics() {
        assertProblem(get("/api/v1/admin/stats/overview"), 401, "AUTHENTICATION_REQUIRED");
        assertProblem(get("/api/v1/admin/stats/overview", KEYS.token(UUID.randomUUID(), "LEARNER", "MENTOR")), 403,
                "ACCESS_DENIED");
        assertProblem(get("/api/v1/admin/stats/timeseries", KEYS.token(UUID.randomUUID(), "MENTOR")), 403,
                "ACCESS_DENIED");
        assertThat(get("/actuator/health").statusCode()).isEqualTo(200);
    }

    private void payment(String type, String paymentId, String occurredAt, long amount, String currency,
                         long refundedAmount) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("paymentId", paymentId);
        fields.put("bookingId", UUID.randomUUID().toString());
        fields.put("payerUserId", UUID.randomUUID().toString());
        fields.put("mentorId", UUID.randomUUID().toString());
        fields.put("amount", amount);
        fields.put("currency", currency);
        fields.put("refundedAmount", refundedAmount);
        publish("payment.events", event(type, occurredAt, fields));
    }

    private void booking(String type, String occurredAt) {
        publish("booking.lifecycle", event(type, occurredAt, Map.of("bookingId", "1", "reason", "x")));
    }

    private byte[] event(String type, String occurredAt, Map<String, Object> fields) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventId", UUID.randomUUID().toString());
        body.put("eventType", type);
        body.put("occurredAt", occurredAt);
        body.putAll(fields);
        return objectMapper.writeValueAsBytes(body);
    }

    private void publish(String routingKey, byte[] body) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType("application/json");
        properties.setMessageId(UUID.randomUUID().toString());
        rabbit.send(EXCHANGE, routingKey, new Message(body, properties));
    }

    private void awaitProcessed(long count) {
        await().atMost(Duration.ofSeconds(15)).until(() -> mongo.getCollection("processed_events").countDocuments() == count);
    }

    private long queueDepth(String queue) {
        var info = rabbitAdmin.getQueueInfo(queue);
        return info == null ? -1 : info.getMessageCount();
    }

    private static JsonNode data(HttpResponse<String> response) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        JsonNode body = json(response);
        assertThat(body.get("success").asBoolean()).isTrue();
        return body.get("data");
    }

    private static void assertProblem(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                type -> assertThat(type).startsWith("application/problem+json"));
        assertThat(json(response).get("code").asString()).isEqualTo(code);
    }
}
