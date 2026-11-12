package com.mentormatching.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mentormatching.notification.domain.DeliveryAttempt;
import com.mentormatching.notification.domain.Notification;
import com.mentormatching.notification.domain.NotificationType;
import com.mentormatching.notification.repository.DeliveryAttemptRepository;
import com.mentormatching.notification.repository.InboxEventRepository;
import com.mentormatching.notification.repository.NotificationRepository;
import com.mentormatching.notification.support.NotificationIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;

/**
 * #81: sự kiện booking/payment/review đúng định dạng các service đó đang publish trên {@code develop}
 * → đúng người nhận, nội dung tiếng Việt, không trùng khi gửi lại, có lịch sử {@code delivery_attempts}.
 */
class BookingPaymentReviewEventsIT extends NotificationIntegrationTestSupport {

    private static final String EXCHANGE = "mentor.events";
    private static final String DEAD_LETTER_QUEUE = "notification.domain-events.dead";

    @Autowired private RabbitTemplate rabbit;
    @Autowired private RabbitAdmin rabbitAdmin;
    @Autowired private NotificationRepository notifications;
    @Autowired private InboxEventRepository inbox;
    @Autowired private DeliveryAttemptRepository deliveryAttempts;
    @Autowired private ObjectMapper objectMapper;

    private final UUID learner = UUID.randomUUID();
    private final UUID mentor = UUID.randomUUID();

    @BeforeEach
    void clean() {
        notifications.deleteAll();
        inbox.deleteAll();
        deliveryAttempts.deleteAll();
        rabbitAdmin.purgeQueue(DEAD_LETTER_QUEUE, false);
        BOOKINGS.reset();
    }

    @Test
    void bookingConfirmedNotifiesLearnerAndMentorResolvedThroughBookingService() {
        BOOKINGS.booking("101", learner, mentor);

        // Đúng payload BookingLifecycleEventMessage của booking-service: chỉ có bookingId (chuỗi) và reason.
        publish("booking.lifecycle", event("BookingConfirmed", Map.of("bookingId", "101",
                "reason", "Payment confirmed successfully")));

        Notification toLearner = awaitOne(learner);
        Notification toMentor = awaitOne(mentor);
        assertThat(toLearner.getType()).isEqualTo(NotificationType.BOOKING_CONFIRMED);
        assertThat(toLearner.getReferenceType()).isEqualTo("BOOKING");
        assertThat(toLearner.getReferenceId()).isEqualTo("101");
        assertThat(toLearner.getTitle()).isEqualTo("Buổi học đã được xác nhận");
        assertThat(toMentor.getType()).isEqualTo(NotificationType.BOOKING_CONFIRMED);
    }

    @Test
    void bookingRejectedAndCancelledCarryTheReason() {
        BOOKINGS.booking("102", learner, mentor);
        BOOKINGS.booking("103", learner, mentor);

        publish("booking.lifecycle", event("BookingRejected", Map.of("bookingId", "102", "reason", "Mentor bận")));
        publish("booking.lifecycle", event("BookingCancelled", Map.of("bookingId", "103",
                "reason", "Hold expired without payment/confirmation")));

        await().atMost(Duration.ofSeconds(15)).until(() -> forUser(learner).size() == 2 && forUser(mentor).size() == 1);
        assertThat(forUser(learner)).anySatisfy(n -> {
            assertThat(n.getType()).isEqualTo(NotificationType.BOOKING_REJECTED);
            assertThat(n.getMessage()).contains("Lý do: Mentor bận");
        });
        assertThat(forUser(mentor)).singleElement()
                .satisfies(n -> assertThat(n.getType()).isEqualTo(NotificationType.BOOKING_CANCELLED));
    }

    @Test
    void paymentEventsNotifyThePayerDirectlyWithoutLookup() {
        UUID paymentId = UUID.randomUUID();
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("paymentId", paymentId.toString());
        fields.put("bookingId", UUID.randomUUID().toString());
        fields.put("payerUserId", learner.toString());
        fields.put("mentorId", UUID.randomUUID().toString());
        fields.put("amount", 500000);
        fields.put("currency", "VND");
        fields.put("refundedAmount", 0);
        publish("payment.events", event("PaymentSucceeded", fields));

        Notification paid = awaitOne(learner);
        assertThat(paid.getType()).isEqualTo(NotificationType.PAYMENT_SUCCESS);
        assertThat(paid.getMessage()).contains("500.000 VND");
        assertThat(paid.getReferenceType()).isEqualTo("PAYMENT");
        assertThat(paid.getReferenceId()).isEqualTo(paymentId.toString());
        assertThat(BOOKINGS.requestCount()).as("payment events carry the payer already").isZero();
    }

    @Test
    void reviewCreatedNotifiesTheMentorAccountResolvedFromTheBooking() {
        BOOKINGS.booking("104", learner, mentor);

        // Đúng payload ReviewCreatedEvent của review-service: mentorId là id hồ sơ (số), không phải tài khoản.
        publish("review.events", event("ReviewCreated", Map.of("reviewId", 9, "bookingId", 104,
                "studentUserId", learner.toString(), "mentorId", 7, "rating", 5)));

        Notification review = awaitOne(mentor);
        assertThat(review.getType()).isEqualTo(NotificationType.REVIEW_CREATED);
        assertThat(review.getMessage()).contains("5★");
        assertThat(forUser(learner)).isEmpty();
    }

    @Test
    void replayedEventDoesNotNotifyTwiceAndDeliveryAttemptsAreRecorded() {
        BOOKINGS.booking("105", learner, mentor);
        byte[] body = event("BookingConfirmed", Map.of("bookingId", "105"));

        publish("booking.lifecycle", body);
        awaitOne(learner);
        publish("booking.lifecycle", body);

        await().pollDelay(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(10))
                .until(() -> queueDepth("notification.domain-events") == 0);
        assertThat(forUser(learner)).hasSize(1);
        assertThat(forUser(mentor)).hasSize(1);
        String notificationId = forUser(learner).getFirst().getId();
        List<DeliveryAttempt> attempts = deliveryAttempts.findByNotificationIdOrderByAttemptAsc(notificationId);
        assertThat(attempts).singleElement().satisfies(a -> {
            assertThat(a.getOutcome()).isEqualTo(DeliveryAttempt.Outcome.SENT);
            assertThat(a.getAttempt()).isEqualTo(1);
            assertThat(a.getRecipientUserId()).isEqualTo(learner.toString());
        });
    }

    @Test
    void unknownBookingGoesToDeadLetterQueueWithoutRetrying() {
        publish("booking.lifecycle", event("BookingConfirmed", Map.of("bookingId", "999")));

        await().atMost(Duration.ofSeconds(10)).until(() -> queueDepth(DEAD_LETTER_QUEUE) == 1);
        assertThat(BOOKINGS.requestCount()).as("404 is not retried").isEqualTo(1);
        assertThat(notifications.count()).isZero();
    }

    @Test
    void bookingServiceOutageIsRetriedThenDeadLettered() {
        BOOKINGS.failWith("106", 503);

        publish("booking.lifecycle", event("BookingCancelled", Map.of("bookingId", "106")));

        await().atMost(Duration.ofSeconds(20)).until(() -> queueDepth(DEAD_LETTER_QUEUE) == 1);
        assertThat(BOOKINGS.requestCount()).as("first try + 1 retry (max-retries=1 in tests)").isEqualTo(2);
        assertThat(notifications.count()).isZero();
        assertThat(inbox.count()).isZero();
    }

    private void publish(String routingKey, byte[] body) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType("application/json");
        properties.setMessageId(UUID.randomUUID().toString());
        rabbit.send(EXCHANGE, routingKey, new Message(body, properties));
    }

    private byte[] event(String type, Map<String, Object> fields) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventId", UUID.randomUUID().toString());
        body.put("eventType", type);
        body.put("occurredAt", "2026-11-10T10:00:00Z");
        body.putAll(fields);
        return objectMapper.writeValueAsBytes(body);
    }

    private List<Notification> forUser(UUID user) {
        return notifications.findAll().stream().filter(n -> n.isOwnedBy(user)).toList();
    }

    private Notification awaitOne(UUID user) {
        return await().atMost(Duration.ofSeconds(15)).until(() -> forUser(user), list -> list.size() == 1).getFirst();
    }

    private long queueDepth(String queue) {
        var info = rabbitAdmin.getQueueInfo(queue);
        return info == null ? -1 : info.getMessageCount();
    }
}
