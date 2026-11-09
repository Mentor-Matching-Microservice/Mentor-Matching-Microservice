package com.mentormatching.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.mentormatching.notification.domain.Notification;
import com.mentormatching.notification.domain.NotificationType;
import com.mentormatching.notification.event.DomainEvent;
import com.mentormatching.notification.repository.InboxEventRepository;
import com.mentormatching.notification.repository.NotificationRepository;
import com.mentormatching.notification.service.NotificationIngestionService;
import com.mentormatching.notification.service.NotificationIngestionService.Outcome;
import com.mentormatching.notification.support.NotificationIntegrationTestSupport;
import com.mentormatching.notification.support.StompTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;

/**
 * Sự kiện đi qua RabbitMQ thật: exchange dùng chung {@code mentor.events} → queue riêng của notification-service.
 */
class DomainEventConsumptionIT extends NotificationIntegrationTestSupport {

    private static final String EXCHANGE = "mentor.events";
    private static final String QUEUE = "notification.domain-events";
    private static final String DEAD_LETTER_QUEUE = "notification.domain-events.dead";

    @Autowired
    private RabbitTemplate rabbit;
    @Autowired
    private RabbitAdmin rabbitAdmin;
    @Autowired
    private NotificationRepository notifications;
    @Autowired
    private InboxEventRepository inbox;
    @Autowired
    private NotificationIngestionService ingestion;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clean() {
        notifications.deleteAll();
        inbox.deleteAll();
        rabbitAdmin.purgeQueue(DEAD_LETTER_QUEUE, false);
    }

    @Test
    void accountRegisteredCreatesWelcomeNotificationAndPushesItRealtime() throws Exception {
        UUID account = UUID.randomUUID();
        try (StompTestClient stomp = new StompTestClient(port)) {
            BlockingQueue<Map<String, Object>> live =
                    StompTestClient.subscribeOwnQueue(stomp.connect(KEYS.token(account, "LEARNER"), null));
            Thread.sleep(300); // SUBSCRIBE được xử lý bất đồng bộ

            publish("auth.account", event("AccountRegistered", Map.of("accountId", account.toString())));

            List<Notification> created = awaitNotificationsFor(account, 1);
            assertThat(created.getFirst().getType()).isEqualTo(NotificationType.ACCOUNT_REGISTERED);
            assertThat(created.getFirst().getTitle()).contains("Chào mừng");
            Map<String, Object> pushed = live.poll(10, TimeUnit.SECONDS);
            assertThat(pushed).as("realtime push after commit").isNotNull();
            assertThat(pushed).containsEntry("id", created.getFirst().getId())
                    .containsEntry("type", "ACCOUNT_REGISTERED").containsEntry("isRead", false);
        }
    }

    @Test
    void mentorApprovedAndRejectedNotifyTheApplicant() {
        UUID approved = UUID.randomUUID();
        UUID rejected = UUID.randomUUID();
        publish("mentor.verification", event("MentorApproved",
                Map.of("mentorUserId", approved.toString(), "mentorId", "17")));
        publish("mentor.verification", event("MentorRejected",
                Map.of("mentorUserId", rejected.toString(), "reason", "Ảnh CCCD bị mờ")));

        Notification ok = awaitNotificationsFor(approved, 1).getFirst();
        assertThat(ok.getType()).isEqualTo(NotificationType.MENTOR_APPROVED);
        assertThat(ok.getReferenceType()).isEqualTo("MENTOR");
        assertThat(ok.getReferenceId()).isEqualTo("17");
        Notification no = awaitNotificationsFor(rejected, 1).getFirst();
        assertThat(no.getType()).isEqualTo(NotificationType.MENTOR_REJECTED);
        assertThat(no.getMessage()).contains("Ảnh CCCD bị mờ");
    }

    @Test
    void redeliveredEventDoesNotCreateDuplicates() {
        UUID account = UUID.randomUUID();
        byte[] body = event("AccountRegistered", Map.of("accountId", account.toString()));
        String eventId = eventIdOf(body);

        publish("auth.account", body);
        awaitNotificationsFor(account, 1);
        publish("auth.account", body);
        publish("auth.account", body);

        await().pollDelay(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(10))
                .until(() -> queueDepth(QUEUE) == 0);
        assertThat(notifications.findAll()).filteredOn(n -> n.isOwnedBy(account)).hasSize(1);
        assertThat(inbox.findById(eventId)).isPresent();
    }

    @Test
    void concurrentDeliveriesOfTheSameEventCreateOneNotification() throws Exception {
        UUID account = UUID.randomUUID();
        DomainEvent event = DomainEvent.parse(event("AccountRegistered", Map.of("accountId", account.toString())),
                objectMapper);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Outcome>> tasks = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                tasks.add(() -> ingestion.ingest(event));
            }
            List<Outcome> outcomes = new ArrayList<>();
            for (Future<Outcome> f : pool.invokeAll(tasks)) {
                outcomes.add(f.get(30, TimeUnit.SECONDS));
            }
            assertThat(outcomes).containsOnlyOnce(Outcome.CREATED);
            assertThat(outcomes).filteredOn(o -> o == Outcome.DUPLICATE).hasSize(3);
        } finally {
            pool.shutdownNow();
        }
        assertThat(notifications.findAll()).filteredOn(n -> n.isOwnedBy(account)).hasSize(1);
    }

    @Test
    void malformedOrIncompleteEventsGoToDeadLetterQueueWithoutRetrying() {
        publish("auth.account", "{not json".getBytes(StandardCharsets.UTF_8));
        publish("auth.account", event("AccountRegistered", Map.of()));               // thiếu accountId
        publish("auth.account", event("AccountRegistered", Map.of("accountId", "x"))); // không phải UUID

        await().atMost(Duration.ofSeconds(5)).until(() -> queueDepth(DEAD_LETTER_QUEUE) == 3);
        assertThat(notifications.count()).isZero();
        assertThat(inbox.count()).isZero();
    }

    @Test
    void unsupportedEventTypesAreAcknowledgedAndIgnored() {
        publish("mentor.rating", event("MentorRatingUpdated", Map.of("mentorId", 5, "averageRating", 4.5)));

        await().pollDelay(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(10))
                .until(() -> queueDepth(QUEUE) == 0);
        assertThat(queueDepth(DEAD_LETTER_QUEUE)).isZero();
        assertThat(notifications.count()).isZero();
        assertThat(inbox.count()).as("ignored events are not recorded").isZero();
    }

    @Test
    void routingKeysOfOtherDomainsAreNotBoundYet() {
        // booking/payment/review được bổ sung ở #81; ở #73 queue chỉ nhận auth.# và mentor.#.
        UUID learner = UUID.randomUUID();
        publish("booking.lifecycle", event("AccountRegistered", Map.of("accountId", learner.toString())));

        await().pollDelay(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(5)).until(() -> queueDepth(QUEUE) == 0);
        assertThat(notifications.count()).isZero();
    }

    private void publish(String routingKey, byte[] body) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType("application/json");
        properties.setMessageId(UUID.randomUUID().toString());
        rabbit.send(EXCHANGE, routingKey, new Message(body, properties));
    }

    private byte[] event(String type, Map<String, Object> fields) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("eventId", UUID.randomUUID().toString());
        body.put("eventType", type);
        body.put("occurredAt", "2026-11-10T10:00:00Z");
        body.putAll(fields);
        return objectMapper.writeValueAsBytes(body);
    }

    private String eventIdOf(byte[] body) {
        return objectMapper.readTree(body).get("eventId").asString();
    }

    private List<Notification> awaitNotificationsFor(UUID user, int expected) {
        return await().atMost(Duration.ofSeconds(15))
                .until(() -> notifications.findAll().stream().filter(n -> n.isOwnedBy(user)).toList(),
                        list -> list.size() == expected);
    }

    private long queueDepth(String queue) {
        var info = rabbitAdmin.getQueueInfo(queue);
        return info == null ? -1 : info.getMessageCount();
    }
}
