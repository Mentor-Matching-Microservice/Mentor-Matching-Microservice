package com.mentormatching.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.mentormatching.auth.entity.Account;
import com.mentormatching.auth.entity.AccountRole;
import com.mentormatching.auth.entity.AccountStatus;
import com.mentormatching.auth.entity.OutboxEvent;
import com.mentormatching.auth.messaging.AccountEvents;
import com.mentormatching.auth.repository.AccountRepository;
import com.mentormatching.auth.repository.AccountRoleRepository;
import com.mentormatching.auth.repository.InboxEventRepository;
import com.mentormatching.auth.repository.OutboxEventRepository;
import com.mentormatching.auth.repository.RoleRepository;
import com.mentormatching.auth.support.AuthIntegrationTestSupport;
import com.mentormatching.auth.support.CapturingMailSender;
import com.mentormatching.auth.support.CapturingMailSender.Kind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.DockerClientFactory;
import tools.jackson.databind.JsonNode;

/**
 * #64 trên MySQL + RabbitMQ thật: outbox, khoá/mở tài khoản, endpoint nội bộ, consumer MentorApproved.
 */
class AccountEventsAndAdminIT extends AuthIntegrationTestSupport {

    private static final String EXCHANGE = "mentor.events";
    private static final String PROBE_QUEUE = "it.auth-events-probe";
    private static final String DLQ = "auth.mentor-events.dead";

    @Autowired private CapturingMailSender mails;
    @Autowired private AccountRepository accounts;
    @Autowired private RoleRepository roles;
    @Autowired private AccountRoleRepository accountRoles;
    @Autowired private OutboxEventRepository outbox;
    @Autowired private InboxEventRepository inbox;
    @Autowired private RabbitTemplate rabbit;
    @Autowired private RabbitAdmin rabbitAdmin;
    private org.springframework.transaction.support.TransactionTemplate transactions;

    @Autowired
    void transactions(org.springframework.transaction.PlatformTransactionManager manager) {
        this.transactions = new org.springframework.transaction.support.TransactionTemplate(manager);
    }

    @BeforeEach
    void probeQueue() {
        // Một "service khác" lắng nghe auth.# để kiểm tra sự kiện thực sự tới broker.
        Queue probe = new Queue(PROBE_QUEUE, false, false, false);
        rabbitAdmin.declareQueue(probe);
        rabbitAdmin.declareBinding(BindingBuilder.bind(probe).to(new TopicExchange(EXCHANGE, true, false)).with("auth.#"));
        rabbitAdmin.purgeQueue(PROBE_QUEUE, false);
        rabbitAdmin.purgeQueue(DLQ, false);
    }

    // ---------- outbox ----------

    @Test
    void registrationPublishesAccountRegisteredThroughTheOutbox() {
        String email = uniqueEmail();
        HttpResponse<String> registered = post("/api/v1/auth/register", registerBody(email, PASSWORD));
        UUID accountId = UUID.fromString(json(registered).get("data").get("accountId").asString());

        OutboxEvent event = await().atMost(Duration.ofSeconds(15)).until(
                () -> outbox.findByAggregateIdOrderByCreatedAtAsc(accountId).stream()
                        .filter(e -> e.getPublishedAt() != null).findFirst().orElse(null), e -> e != null);
        assertThat(event.getEventType()).isEqualTo(AccountEvents.ACCOUNT_REGISTERED);
        assertThat(event.getRoutingKey()).isEqualTo("auth.account.registered");

        JsonNode received = receive(event.getId());
        assertThat(received.get("eventType").asString()).isEqualTo("AccountRegistered");
        assertThat(received.get("accountId").asString()).isEqualTo(accountId.toString());
        assertThat(received.get("email").asString()).isEqualTo(email);
        assertThat(received.get("status").asString()).isEqualTo("PENDING_VERIFICATION");
        assertThat(received.get("roles").toString()).contains("LEANER");
    }

    @Test
    void outboxKeepsEventsWhileTheBrokerIsDownAndSendsThemAfterRecovery() {
        var docker = DockerClientFactory.instance().client();
        String containerId = RABBIT.getContainerId();
        docker.pauseContainerCmd(containerId).exec();
        UUID accountId;
        try {
            HttpResponse<String> registered = post("/api/v1/auth/register", registerBody(uniqueEmail(), PASSWORD));
            assertThat(registered.statusCode()).as("registration does not depend on the broker").isEqualTo(201);
            accountId = UUID.fromString(json(registered).get("data").get("accountId").asString());
            UUID id = accountId;
            // Publisher gửi theo thứ tự tạo và dừng ở sự kiện cũ nhất chưa đi được, nên lần thử thất bại được ghi
            // trên sự kiện đứng đầu hàng chờ (có thể là sự kiện của test khác); sự kiện của test này phải còn chờ.
            await().atMost(Duration.ofSeconds(30)).until(() -> outbox.findAll().stream()
                    .anyMatch(e -> e.getAttempts() > 0 && e.getPublishedAt() == null));
            assertThat(outbox.findByAggregateIdOrderByCreatedAtAsc(id)).allMatch(e -> e.getPublishedAt() == null);
        } finally {
            docker.unpauseContainerCmd(containerId).exec();
        }
        UUID id = accountId;
        OutboxEvent delivered = await().atMost(Duration.ofSeconds(90)).until(
                () -> outbox.findByAggregateIdOrderByCreatedAtAsc(id).stream()
                        .filter(e -> e.getPublishedAt() != null).findFirst().orElse(null), e -> e != null);
        assertThat(receive(delivered.getId()).get("accountId").asString()).isEqualTo(accountId.toString());
        assertThat(outbox.countByPublishedAtIsNull()).as("whole backlog delivered after recovery").isZero();
    }

    // ---------- admin status ----------

    @Test
    void adminLocksAnAccountRevokesItsSessionsAndPublishesTheChange() {
        String adminToken = adminAccessToken();
        String email = activeAccount();
        UUID accountId = accounts.findByLoginEmail(email).orElseThrow().getId();
        String refresh = refreshCookie(post("/api/v1/auth/login", loginBody(email, PASSWORD)));

        HttpResponse<String> locked = patchStatus(adminToken, accountId, "{\"status\":\"LOCKED\",\"reason\":\"Spam\"}");
        assertThat(locked.statusCode()).as(locked.body()).isEqualTo(200);
        JsonNode data = json(locked).get("data");
        assertThat(data.get("status").asString()).isEqualTo("LOCKED");
        assertThat(data.get("revokedRefreshTokens").asInt()).isGreaterThanOrEqualTo(1);
        long lockedVersion = data.get("aggregateVersion").asLong();

        assertThat(post("/api/v1/auth/refresh-token", null, refresh).statusCode()).as("sessions revoked").isEqualTo(401);
        assertThat(post("/api/v1/auth/login", loginBody(email, PASSWORD)).statusCode()).isEqualTo(401);

        JsonNode changed = publishedStatusChange(accountId, "LOCKED");
        assertThat(changed.get("previousStatus").asString()).isEqualTo("ACTIVE");
        assertThat(changed.get("aggregateVersion").asLong()).isEqualTo(lockedVersion);
        assertThat(changed.get("reason").asString()).isEqualTo("Spam");

        // Đặt lại cùng trạng thái: không đổi gì, không phát thêm sự kiện.
        long eventsBefore = statusEvents(accountId);
        assertThat(patchStatus(adminToken, accountId, "{\"status\":\"LOCKED\"}").statusCode()).isEqualTo(200);
        assertThat(statusEvents(accountId)).isEqualTo(eventsBefore);

        HttpResponse<String> reactivated = patchStatus(adminToken, accountId, "{\"status\":\"ACTIVE\"}");
        assertThat(json(reactivated).get("data").get("aggregateVersion").asLong()).isGreaterThan(lockedVersion);
        assertThat(post("/api/v1/auth/login", loginBody(email, PASSWORD)).statusCode()).isEqualTo(200);
        assertThat(publishedStatusChange(accountId, "ACTIVE").get("previousStatus").asString()).isEqualTo("LOCKED");
    }

    @Test
    void adminStatusApiIsRestrictedAndValidated() {
        String adminToken = adminAccessToken();
        UUID adminId = UUID.fromString(org.springframework.security.oauth2.jwt.NimbusJwtDecoder
                .withJwkSetUri(baseUrl() + "/.well-known/jwks.json").build().decode(adminToken).getSubject());
        String email = activeAccount();
        UUID target = accounts.findByLoginEmail(email).orElseThrow().getId();
        String learnerToken = json(post("/api/v1/auth/login", loginBody(email, PASSWORD))).get("data").get("accessToken").asString();

        assertProblem(patchStatus(null, target, "{\"status\":\"LOCKED\"}"), 401, "AUTHENTICATION_REQUIRED");
        assertProblem(patchStatus("not.a.jwt", target, "{\"status\":\"LOCKED\"}"), 401, "AUTHENTICATION_REQUIRED");
        assertProblem(patchStatus(learnerToken, target, "{\"status\":\"LOCKED\"}"), 403, "ACCESS_DENIED");
        assertProblem(patchStatus(adminToken, target, "{\"status\":\"PENDING_VERIFICATION\"}"), 400, "INVALID_REQUEST");
        assertThat(patchStatus(adminToken, target, "{\"status\":\"FROZEN\"}").statusCode()).isEqualTo(400);
        assertProblem(patchStatus(adminToken, adminId, "{\"status\":\"DISABLED\"}"), 409, "CONFLICT");
        assertProblem(patchStatus(adminToken, UUID.randomUUID(), "{\"status\":\"LOCKED\"}"), 404, "RESOURCE_NOT_FOUND");
        assertThat(accounts.findById(target).orElseThrow().getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    // ---------- internal status ----------

    @Test
    void internalStatusEndpointRequiresTheServiceToken() {
        String email = activeAccount();
        UUID accountId = accounts.findByLoginEmail(email).orElseThrow().getId();

        HttpResponse<String> ok = send(request("/internal/v1/accounts/" + accountId + "/status")
                .header("Authorization", "Bearer " + INTERNAL_TOKEN).GET().build());
        assertThat(ok.statusCode()).isEqualTo(200);
        assertThat(json(ok).get("status").asString()).isEqualTo("ACTIVE");
        assertThat(json(ok).get("userId").asString()).isEqualTo(accountId.toString());
        assertThat(ok.body()).doesNotContain(email);

        assertThat(send(request("/internal/v1/accounts/" + accountId + "/status").GET().build()).statusCode()).isEqualTo(401);
        assertThat(send(request("/internal/v1/accounts/" + UUID.randomUUID() + "/status")
                .header("Authorization", "Bearer " + INTERNAL_TOKEN).GET().build()).statusCode()).isEqualTo(404);
    }

    // ---------- MentorApproved consumer ----------

    @Test
    void mentorApprovedGrantsTheMentorRoleExactlyOnce() {
        String email = activeAccount();
        UUID accountId = accounts.findByLoginEmail(email).orElseThrow().getId();
        String eventId = UUID.randomUUID().toString();
        byte[] body = ("{\"eventId\":\"" + eventId + "\",\"eventType\":\"MentorApproved\",\"mentorUserId\":\""
                + accountId + "\",\"mentorId\":12}").getBytes(StandardCharsets.UTF_8);

        publish("mentor.verification.approved", body);
        publish("mentor.verification.approved", body);
        await().atMost(Duration.ofSeconds(15)).until(() -> roleCodes(accountId).contains("MENTOR"));
        await().pollDelay(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(10))
                .until(() -> queueDepth("auth.mentor-events") == 0);

        assertThat(roleCodes(accountId)).containsExactlyInAnyOrder("LEANER", "MENTOR");
        assertThat(inbox.existsById(eventId)).isTrue();
        assertThat(queueDepth(DLQ)).isZero();
    }

    @Test
    void invalidMentorEventsAreDeadLetteredAndOthersIgnored() {
        publish("mentor.verification.approved", "{broken".getBytes(StandardCharsets.UTF_8));
        publish("mentor.verification.approved", ("{\"eventId\":\"" + UUID.randomUUID()
                + "\",\"eventType\":\"MentorApproved\",\"mentorUserId\":\"" + UUID.randomUUID() + "\"}").getBytes(StandardCharsets.UTF_8));
        publish("mentor.rating", ("{\"eventId\":\"" + UUID.randomUUID()
                + "\",\"eventType\":\"MentorRatingUpdated\",\"mentorId\":3}").getBytes(StandardCharsets.UTF_8));

        await().atMost(Duration.ofSeconds(15)).until(() -> queueDepth(DLQ) == 2);
    }

    // ---------- helpers ----------

    private String activeAccount() {
        String email = uniqueEmail();
        post("/api/v1/auth/register", registerBody(email, PASSWORD));
        post("/api/v1/auth/verify-email", "{\"token\":\"" + mails.latestToken(email, Kind.VERIFY_EMAIL) + "\"}");
        return email;
    }

    /** Admin cho test: tài khoản thường được gán thêm role ADMIN trực tiếp trong DB (chỉ ở test). */
    private String adminAccessToken() {
        String email = activeAccount();
        Account account = accounts.findByLoginEmail(email).orElseThrow();
        AccountRole admin = new AccountRole();
        admin.setAccount(account);
        admin.setRole(roles.findByCode("ADMIN").orElseThrow());
        admin.setAssignedAt(Instant.now());
        accountRoles.save(admin);
        return json(post("/api/v1/auth/login", loginBody(email, PASSWORD))).get("data").get("accessToken").asString();
    }

    private HttpResponse<String> patchStatus(String token, UUID accountId, String body) {
        HttpRequest.Builder builder = request("/api/v1/admin/accounts/" + accountId + "/status")
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return send(builder.build());
    }

    private JsonNode publishedStatusChange(UUID accountId, String status) {
        OutboxEvent event = await().atMost(Duration.ofSeconds(15)).until(
                () -> outbox.findByAggregateIdOrderByCreatedAtAsc(accountId).stream()
                        .filter(e -> e.getEventType().equals(AccountEvents.ACCOUNT_STATUS_CHANGED))
                        .filter(e -> e.getPublishedAt() != null
                                && status.equals(JSON.readTree(e.getPayload()).get("status").asString()))
                        .reduce((a, b) -> b).orElse(null), e -> e != null);
        assertThat(event.getRoutingKey()).isEqualTo("auth.account.status-changed");
        return receive(event.getId());
    }

    private long statusEvents(UUID accountId) {
        return outbox.findByAggregateIdOrderByCreatedAtAsc(accountId).stream()
                .filter(e -> e.getEventType().equals(AccountEvents.ACCOUNT_STATUS_CHANGED)).count();
    }

    /** Lấy đúng message có messageId = eventId từ queue thăm dò (bỏ qua message khác). */
    private JsonNode receive(UUID eventId) {
        return await().atMost(Duration.ofSeconds(15)).until(() -> {
            Message message;
            while ((message = rabbit.receive(PROBE_QUEUE, 200)) != null) {
                if (eventId.toString().equals(message.getMessageProperties().getMessageId())) {
                    return JSON.readTree(message.getBody());
                }
            }
            return null;
        }, node -> node != null);
    }

    private void publish(String routingKey, byte[] body) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType("application/json");
        rabbit.send(EXCHANGE, routingKey, new Message(body, properties));
    }

    private List<String> roleCodes(UUID accountId) {
        return transactions.execute(status -> accountRoles.findByAccount_Id(accountId).stream()
                .map(r -> r.getRole().getCode()).toList());
    }

    private long queueDepth(String queue) {
        var info = rabbitAdmin.getQueueInfo(queue);
        return info == null ? -1 : info.getMessageCount();
    }

    private static void assertProblem(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        assertThat(json(response).get("code").asString()).isEqualTo(code);
    }
}
