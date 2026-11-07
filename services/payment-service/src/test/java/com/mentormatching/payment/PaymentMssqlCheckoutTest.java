package com.mentormatching.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import com.mentormatching.payment.api.PaymentModels.BookingPaymentContext;
import com.mentormatching.payment.domain.BookingEventReceiptRepository;
import com.mentormatching.payment.domain.OutboxEventRepository;
import com.mentormatching.payment.domain.Payment;
import com.mentormatching.payment.domain.PaymentAttemptRepository;
import com.mentormatching.payment.domain.PaymentRepository;
import com.mentormatching.payment.domain.PaymentStatus;
import com.mentormatching.payment.service.BookingLifecycleEvent;
import com.mentormatching.payment.service.BookingPaymentSummaryClient;
import com.mentormatching.payment.service.PaymentService;
import com.stripe.Stripe;
import com.mentormatching.payment.service.StripePaymentClient;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {"EUREKA_ENABLED=false", "payment.messaging.enabled=false",
        "payment.stripe.webhook-secret=whsec_checkout_test"})
@AutoConfigureMockMvc
@Testcontainers
class PaymentMssqlCheckoutTest {
    @Container
    static final MSSQLServerContainer<?> SQL_SERVER = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
            .acceptLicense()
            .withPassword("Mssql-Test!Passw0rd");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", SQL_SERVER::getJdbcUrl);
        properties.add("spring.datasource.username", SQL_SERVER::getUsername);
        properties.add("spring.datasource.password", SQL_SERVER::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PaymentRepository payments;
    @Autowired PaymentAttemptRepository attempts;
    @Autowired OutboxEventRepository outbox;
    @Autowired BookingEventReceiptRepository bookingEventReceipts;
    @Autowired PaymentService paymentService;
    @MockitoBean BookingPaymentSummaryClient bookingClient;
    @MockitoBean StripePaymentClient stripe;

    private UUID learnerId;
    private UUID mentorId;
    private UUID bookingId;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM webhook_receipts");
        jdbc.update("DELETE FROM booking_event_receipts");
        jdbc.update("DELETE FROM outbox_events");
        jdbc.update("DELETE FROM refund_transactions");
        jdbc.update("DELETE FROM payment_attempts");
        jdbc.update("DELETE FROM payments");
        learnerId = UUID.randomUUID();
        mentorId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        when(stripe.webhookSecret()).thenReturn("whsec_checkout_test");
    }

    @Test
    void flywayCreatesRequiredSqlServerTables() {
        assertThat(tableExists("payments")).isTrue();
        assertThat(tableExists("payment_attempts")).isTrue();
        assertThat(tableExists("webhook_receipts")).isTrue();
        assertThat(tableExists("outbox_events")).isTrue();
        assertThat(tableExists("booking_event_receipts")).isTrue();
    }

    @Test
    void sqlServerAllowsSeveralPaymentsBeforeStripeAssignsSessionIds() {
        for (int index = 0; index < 2; index++) {
            jdbc.update("INSERT INTO payments (id, booking_id, payer_id, mentor_id, amount, currency, status, idempotency_key) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    new BigDecimal("75.00"), "USD", "PENDING", "pending-" + index);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM payments", Integer.class)).isEqualTo(2);
    }

    @Test
    void createsOneCheckoutForRepeatedIdempotentRequest() throws Exception {
        when(bookingClient.get(eq(bookingId), anyString())).thenReturn(new BookingPaymentContext(
                bookingId, learnerId, mentorId, "PENDING", new BigDecimal("75.00"), "USD",
                OffsetDateTime.now().plusMinutes(10)));
        Session session = org.mockito.Mockito.mock(Session.class);
        when(session.getId()).thenReturn("cs_test_once");
        when(session.getUrl()).thenReturn("https://checkout.stripe.com/cs_test_once");
        when(session.getExpiresAt()).thenReturn(InstantHolder.expiresAt());
        when(stripe.checkout(any(Payment.class))).thenReturn(session);

        for (int request = 0; request < 2; request++) {
            mvc.perform(post("/api/v1/payments")
                            .with(jwt().jwt(token -> token.subject(learnerId.toString())
                                    .issuer("auth-service").audience(List.of("mentor-matching-api"))))
                            .header("Idempotency-Key", "checkout-request-1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"bookingId\":\"" + bookingId + "\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.bookingId").value(bookingId.toString()))
                    .andExpect(jsonPath("$.checkoutUrl").value("https://checkout.stripe.com/cs_test_once"));
        }

        assertThat(payments.findByBookingId(bookingId)).isPresent();
        assertThat(attempts.count()).isEqualTo(1);
        verify(stripe, times(1)).checkout(any(Payment.class));
        verify(bookingClient, times(1)).get(eq(bookingId), anyString());
    }

    @Test
    void mentorHistoryRequiresMentorRoleAndReturnsMentorsOwnPayments() throws Exception {
        UUID paymentId = UUID.randomUUID();
        jdbc.update("INSERT INTO payments (id, booking_id, payer_id, mentor_id, amount, currency, status, idempotency_key) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                paymentId, bookingId, learnerId, mentorId, new BigDecimal("75.00"), "USD", "PAID", "history-test");

        mvc.perform(get("/api/v1/payments/mentor/me")
                        .with(jwt().jwt(token -> token.subject(mentorId.toString())
                                .issuer("auth-service").audience(List.of("mentor-matching-api")))
                                .authorities(new SimpleGrantedAuthority("ROLE_MENTOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].mentorId").value(mentorId.toString()));

        mvc.perform(get("/api/v1/payments/mentor/me")
                        .with(jwt().jwt(token -> token.subject(learnerId.toString())
                                .issuer("auth-service").audience(List.of("mentor-matching-api")))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsWebhookWithInvalidSignature() {
        assertThatThrownBy(() -> paymentService.handleStripeWebhook("{}", "t=1,v1=invalid"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode().value())
                .isEqualTo(400);
    }

    @Test
    void storesDuplicateStripeWebhookOnlyOnce() throws Exception {
        String payload = """
                {"id":"evt_duplicate_test","object":"event","api_version":"%s","created":1780000000,
                 "data":{"object":{"id":"cus_duplicate_test","object":"customer"}},
                 "livemode":false,"pending_webhooks":1,"type":"customer.created"}
                """.formatted(Stripe.API_VERSION);
        String signature = sign(payload, "whsec_checkout_test");

        paymentService.handleStripeWebhook(payload, signature);
        paymentService.handleStripeWebhook(payload, signature);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM webhook_receipts WHERE provider_event_id = ?",
                Integer.class, "evt_duplicate_test")).isEqualTo(1);
    }

    @Test
    void refundsPaidBookingOnceWhenCancellationEventIsRedelivered() {
        Payment payment = new Payment(bookingId, learnerId, mentorId, new BigDecimal("75.00"), "USD", "paid-test");
        payment.paid("pi_booking_cancel");
        payment = payments.saveAndFlush(payment);
        Refund refund = org.mockito.Mockito.mock(Refund.class);
        when(refund.getId()).thenReturn("re_booking_cancel");
        when(stripe.refund(any(Payment.class), eq(new BigDecimal("75.00")), eq("booking-event:booking-event-1"),
                anyString())).thenReturn(refund);
        BookingLifecycleEvent event = new BookingLifecycleEvent("booking-event-1", "BookingCancelled",
                Instant.now(), bookingId, "Mentor cancelled the session");

        paymentService.handleBookingLifecycleEvent(event);
        paymentService.handleBookingLifecycleEvent(event);

        Payment refunded = payments.findById(payment.getId()).orElseThrow();
        assertThat(refunded.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(refunded.getRefundedAmount()).isEqualByComparingTo("75.00");
        assertThat(bookingEventReceipts.count()).isEqualTo(1);
        assertThat(outbox.count()).isEqualTo(1);
        verify(stripe, times(1)).refund(any(Payment.class), any(BigDecimal.class), anyString(), anyString());
    }

    private static String sign(String payload, String secret) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
        StringBuilder signature = new StringBuilder();
        for (byte value : digest) signature.append(String.format("%02x", value));
        return "t=" + timestamp + ",v1=" + signature;
    }

    private boolean tableExists(String name) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'dbo' AND TABLE_NAME = ?",
                Integer.class, name);
        return count != null && count == 1;
    }

    private static final class InstantHolder {
        private static Long expiresAt() { return java.time.Instant.now().plusSeconds(600).getEpochSecond(); }
    }
}
