package com.mentormatching.payment.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentormatching.payment.api.PaymentModels.BookingPaymentContext;
import com.mentormatching.payment.config.PaymentProperties;

@Component
public class BookingPaymentSummaryClient {
    private final PaymentProperties config;
    private final RestClient http;
    private final ObjectMapper objectMapper;

    public BookingPaymentSummaryClient(PaymentProperties config, ObjectMapper objectMapper) {
        this(config, objectMapper, RestClient.builder().build());
    }

    public BookingPaymentSummaryClient(PaymentProperties config, ObjectMapper objectMapper, RestClient restClient) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.http = restClient != null ? restClient : RestClient.create();
    }

    public BookingPaymentContext get(UUID bookingId, String bearerToken) {
        try {
            String pathId = bookingId != null
                    ? (bookingId.getMostSignificantBits() == 0L && bookingId.getLeastSignificantBits() > 0L
                        ? String.valueOf(bookingId.getLeastSignificantBits())
                        : bookingId.toString())
                    : "";

            String rawJson = http.get()
                    .uri(config.bookingServiceUrl() + "/internal/v1/bookings/{id}/payment-context", pathId)
                    .headers(h -> {
                        if (bearerToken != null && !bearerToken.isBlank()) {
                            h.setBearerAuth(bearerToken);
                        }
                    })
                    .retrieve()
                    .body(String.class);

            if (rawJson == null || rawJson.isBlank()) {
                return null;
            }

            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode data = root.has("data") && root.get("data").isObject() ? root.get("data") : root;

            UUID parsedBookingId = bookingId;
            if (data.has("bookingId") && !data.get("bookingId").isNull()) {
                String rawBid = data.get("bookingId").asText();
                try {
                    parsedBookingId = UUID.fromString(rawBid);
                } catch (IllegalArgumentException e) {
                    try {
                        long numId = Long.parseLong(rawBid);
                        parsedBookingId = new UUID(0L, numId);
                    } catch (NumberFormatException ignored) {}
                }
            }

            if (bookingId != null && parsedBookingId != null) {
                if (bookingId.getLeastSignificantBits() == parsedBookingId.getLeastSignificantBits()) {
                    parsedBookingId = bookingId;
                }
            }

            UUID payerUserId = null;
            if (data.has("payerUserId") && !data.get("payerUserId").isNull()) {
                payerUserId = UUID.fromString(data.get("payerUserId").asText());
            } else if (data.has("studentUserId") && !data.get("studentUserId").isNull()) {
                payerUserId = UUID.fromString(data.get("studentUserId").asText());
            }

            UUID mentorId = null;
            if (data.has("mentorUserId") && !data.get("mentorUserId").isNull()) {
                try {
                    mentorId = UUID.fromString(data.get("mentorUserId").asText());
                } catch (Exception ignored) {}
            }
            if (mentorId == null && data.has("mentorId") && !data.get("mentorId").isNull()) {
                try {
                    mentorId = UUID.fromString(data.get("mentorId").asText());
                } catch (Exception e) {
                    try {
                        mentorId = new UUID(0L, data.get("mentorId").asLong());
                    } catch (Exception ignored) {}
                }
            }

            String bookingStatus = data.has("bookingStatus") ? data.get("bookingStatus").asText()
                    : (data.has("status") ? data.get("status").asText() : null);

            BigDecimal totalAmount = data.has("totalAmount") && !data.get("totalAmount").isNull()
                    ? new BigDecimal(data.get("totalAmount").asText())
                    : null;
            String currency = data.has("currency") && !data.get("currency").isNull()
                    ? data.get("currency").asText()
                    : "VND";

            OffsetDateTime holdExpiresAt = null;
            if (data.has("holdExpiresAt") && !data.get("holdExpiresAt").isNull()) {
                holdExpiresAt = OffsetDateTime.parse(data.get("holdExpiresAt").asText());
            }

            return new BookingPaymentContext(parsedBookingId, payerUserId, mentorId, bookingStatus, totalAmount, currency, holdExpiresAt);
        }
        catch (HttpClientErrorException.Forbidden e) { throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Booking does not belong to current learner", e); }
        catch (HttpClientErrorException.NotFound e) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found", e); }
        catch (HttpClientErrorException.Conflict e) { throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking is no longer payable", e); }
        catch (ResponseStatusException e) { throw e; }
        catch (Exception e) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Booking service could not provide payment details", e); }
    }
}
