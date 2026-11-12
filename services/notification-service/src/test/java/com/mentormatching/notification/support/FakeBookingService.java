package com.mentormatching.notification.support;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Giả lập API nội bộ của booking-service ({@code GET /internal/v1/bookings/{id}/payment-context}) với đúng
 * hình dạng response thật ({@code ApiResponse<BookingPaymentContextResponse>}). Chỉ dùng trong test.
 */
public final class FakeBookingService {

    private static final Pattern PATH = Pattern.compile("/internal/v1/bookings/(\\d+)/payment-context");

    private final HttpServer server;
    private final Map<String, Object> bookings = new ConcurrentHashMap<>();
    private final AtomicInteger requests = new AtomicInteger();

    public FakeBookingService() {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        server.createContext("/", this::handle);
        server.start();
    }

    public String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    /** Booking tồn tại với học viên/mentor cho trước. */
    public void booking(String bookingId, UUID learner, UUID mentor) {
        bookings.put(bookingId, new UUID[] {learner, mentor});
    }

    /** Booking-service trả lỗi HTTP này cho booking (ví dụ 500 để mô phỏng sự cố). */
    public void failWith(String bookingId, int status) {
        bookings.put(bookingId, status);
    }

    public int requestCount() {
        return requests.get();
    }

    public void reset() {
        bookings.clear();
        requests.set(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        requests.incrementAndGet();
        Matcher matcher = PATH.matcher(exchange.getRequestURI().getPath());
        if (!"GET".equals(exchange.getRequestMethod()) || !matcher.matches()) {
            respond(exchange, 404, "{\"status\":404}");
            return;
        }
        Object booking = bookings.get(matcher.group(1));
        if (booking == null) {
            respond(exchange, 404, "{\"status\":404,\"code\":\"BOOKING_NOT_FOUND\"}");
        } else if (booking instanceof Integer status) {
            respond(exchange, status, "{\"status\":" + status + "}");
        } else {
            UUID[] people = (UUID[]) booking;
            respond(exchange, 200, """
                    {"status":200,"code":"SUCCESS","success":true,"message":"ok","data":{
                      "bookingId":%s,"studentUserId":"%s","payerUserId":"%s","mentorId":7,"mentorUserId":"%s",
                      "totalAmount":500000,"currency":"VND","bookingStatus":"CONFIRMED","status":"CONFIRMED"}}
                    """.formatted(matcher.group(1), people[0], people[0], people[1]));
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
