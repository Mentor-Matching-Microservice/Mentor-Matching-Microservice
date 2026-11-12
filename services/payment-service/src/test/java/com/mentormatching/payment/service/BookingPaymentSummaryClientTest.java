package com.mentormatching.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentormatching.payment.api.PaymentModels.BookingPaymentContext;
import com.mentormatching.payment.config.PaymentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class BookingPaymentSummaryClientTest {

    private MockRestServiceServer mockServer;
    private BookingPaymentSummaryClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        PaymentProperties properties = new PaymentProperties(
                "http://localhost:8084",
                new PaymentProperties.Jwt("http://localhost:8081", "mentor-matching", "classpath:jwt/public.pem"),
                new PaymentProperties.Stripe("sk_test_123", "whsec_123", "http://localhost:5173/payment/success",
                        "http://localhost:5173/payment/cancel", Duration.ofMinutes(15))
        );
        client = new BookingPaymentSummaryClient(properties, objectMapper, builder.build());
    }

    @Test
    void get_successfullyParsesWrappedApiResponseFromBookingService() {
        UUID bookingId = new UUID(0L, 10L);
        UUID payerId = UUID.randomUUID();
        UUID mentorId = UUID.randomUUID();
        OffsetDateTime holdExpiresAt = OffsetDateTime.parse("2026-11-12T12:00:00+07:00");

        String bookingServiceResponse = """
                {
                    "success": true,
                    "message": "Get booking payment context successfully",
                    "data": {
                        "bookingId": 10,
                        "studentUserId": "%s",
                        "payerUserId": "%s",
                        "mentorId": 5,
                        "mentorUserId": "%s",
                        "totalAmount": 250000.00,
                        "currency": "VND",
                        "bookingStatus": "PENDING",
                        "status": "PENDING",
                        "holdExpiresAt": "%s"
                    }
                }
                """.formatted(payerId, payerId, mentorId, holdExpiresAt);

        mockServer.expect(requestTo("http://localhost:8084/internal/v1/bookings/10/payment-context"))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withSuccess(bookingServiceResponse, MediaType.APPLICATION_JSON));

        BookingPaymentContext context = client.get(bookingId, "test-token");

        mockServer.verify();
        assertThat(context).isNotNull();
        assertThat(context.bookingId()).isEqualTo(bookingId);
        assertThat(context.payerUserId()).isEqualTo(payerId);
        assertThat(context.mentorId()).isEqualTo(mentorId);
        assertThat(context.bookingStatus()).isEqualTo("PENDING");
        assertThat(context.totalAmount()).isEqualByComparingTo(new BigDecimal("250000.00"));
        assertThat(context.currency()).isEqualTo("VND");
        assertThat(context.holdExpiresAt()).isEqualTo(holdExpiresAt);
    }

    @Test
    void get_translatesNotFoundTo404() {
        UUID bookingId = new UUID(0L, 999L);
        mockServer.expect(requestTo("http://localhost:8084/internal/v1/bookings/999/payment-context"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.get(bookingId, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Booking not found");

        mockServer.verify();
    }
}
