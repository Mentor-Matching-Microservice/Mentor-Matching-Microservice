package com.mentormatching.notification.client;

import java.util.UUID;

import com.mentormatching.notification.event.InvalidEventException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Tra học viên/mentor của một booking qua API nội bộ có sẵn của booking-service.
 * Sự kiện booking và review chỉ mang {@code bookingId}, nên cần bước này để biết gửi thông báo cho ai.
 *
 * <ul>
 *   <li>Booking không tồn tại (404) hoặc dữ liệu không hợp lệ → {@link InvalidEventException} (không thử lại).</li>
 *   <li>Timeout, 5xx, mất kết nối → {@link RecipientLookupException} (thử lại rồi mới vào dead-letter queue).</li>
 * </ul>
 */
@Component
@EnableConfigurationProperties(BookingClientProperties.class)
public class BookingParticipantsClient {

    static final String PATH = "/internal/v1/bookings/{bookingId}/payment-context";

    private final RestClient rest;
    private final ObjectMapper objectMapper;

    public BookingParticipantsClient(BookingClientProperties properties, ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());
        this.rest = RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(requestFactory).build();
        this.objectMapper = objectMapper;
    }

    public BookingParticipants find(String bookingId) {
        if (bookingId == null || !bookingId.matches("\\d{1,19}")) {
            throw new InvalidEventException("bookingId must be a numeric booking id");
        }
        String body;
        try {
            body = rest.get().uri(PATH, bookingId).accept(MediaType.APPLICATION_JSON).retrieve().body(String.class);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new InvalidEventException("Booking not found");
            }
            if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                throw new RecipientLookupException("Booking service is throttling", ex);
            }
            throw new InvalidEventException("Booking lookup rejected with status " + ex.getStatusCode().value());
        } catch (RestClientException ex) {
            throw new RecipientLookupException("Booking service is unavailable", ex);
        }
        return parse(bookingId, body);
    }

    private BookingParticipants parse(String bookingId, String body) {
        JsonNode data;
        try {
            JsonNode root = body == null ? null : objectMapper.readTree(body);
            data = root == null ? null : root.get("data");
        } catch (JacksonException ex) {
            throw new RecipientLookupException("Booking service returned an unreadable response", ex);
        }
        if (data == null || !data.isObject()) {
            throw new RecipientLookupException("Booking service returned no booking data", null);
        }
        return new BookingParticipants(bookingId, uuid(data, "studentUserId"), uuid(data, "mentorUserId"));
    }

    private static UUID uuid(JsonNode data, String field) {
        JsonNode value = data.get(field);
        if (value == null || !value.isString() || value.asString().isBlank()) {
            throw new InvalidEventException("Booking has no " + field);
        }
        try {
            return UUID.fromString(value.asString().strip());
        } catch (IllegalArgumentException ex) {
            throw new InvalidEventException("Booking " + field + " is not a UUID");
        }
    }
}
