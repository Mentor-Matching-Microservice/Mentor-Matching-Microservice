package com.mentormatching.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.mentormatching.user.entity.UserProfile;
import com.mentormatching.user.repository.InboxEventRepository;
import com.mentormatching.user.repository.UserProfileRepository;
import com.mentormatching.user.service.ProvisionUserProfileUseCase;
import com.mentormatching.user.service.ProvisionUserProfileUseCase.Outcome;
import com.mentormatching.user.support.UserIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * #65 trên MySQL + RabbitMQ thật: AccountRegistered (đúng định dạng auth-service phát) → hồ sơ người dùng.
 */
class UserProvisioningIT extends UserIntegrationTestSupport {

    private static final String QUEUE = "user.account-events";
    private static final String DLQ = "user.account-events.dead";

    @Autowired private UserProfileRepository profiles;
    @Autowired private InboxEventRepository inbox;
    @Autowired private ProvisionUserProfileUseCase provision;

    @BeforeEach
    void clean() {
        rabbitAdmin.purgeQueue(DLQ, false);
    }

    @Test
    void accountRegisteredCreatesTheUserProfile() {
        UUID accountId = UUID.randomUUID();
        publish("auth.account.registered", "AccountRegistered", Map.of("accountId", accountId.toString(),
                "email", "New.Learner@Example.com", "status", "PENDING_VERIFICATION", "roles", List.of("LEANER")));

        UserProfile profile = awaitProfile(accountId);
        assertThat(profile.getEmail()).isEqualTo("new.learner@example.com");
        assertThat(profile.getFullName()).isNull();

        HttpResponse<String> me = get("/api/v1/users/me", KEYS.token(accountId, "LEANER"));
        assertThat(me.statusCode()).as(me.body()).isEqualTo(200);
        assertThat(json(me).get("data").get("userId").asString()).isEqualTo(accountId.toString());
    }

    @Test
    void redeliveredEventCreatesOneProfile() {
        UUID accountId = UUID.randomUUID();
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> fields = Map.of("accountId", accountId.toString(), "email", "dup@example.com");
        publishWithId("auth.account.registered", eventId, "AccountRegistered", fields);
        publishWithId("auth.account.registered", eventId, "AccountRegistered", fields);

        awaitProfile(accountId);
        await().pollDelay(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(10)).until(() -> queueDepth(QUEUE) == 0);
        assertThat(inbox.existsById(eventId)).isTrue();
        assertThat(profiles.findAll()).filteredOn(p -> p.getUserId().equals(accountId)).hasSize(1);
        assertThat(queueDepth(DLQ)).isZero();
    }

    @Test
    void concurrentDeliveriesProvisionOnce() throws Exception {
        UUID accountId = UUID.randomUUID();
        String eventId = UUID.randomUUID().toString();
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Outcome>> tasks = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                tasks.add(() -> provision.provision(eventId, "AccountRegistered", accountId, "race@example.com"));
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
    }

    @Test
    void profileSavedByTheUserBeforeTheEventKeepsTheUsersData() {
        UUID accountId = UUID.randomUUID();
        String token = KEYS.token(accountId, "LEANER");
        String phone = "09" + String.valueOf(System.nanoTime()).substring(5, 13);
        HttpResponse<String> saved = send("PUT", "/api/v1/users/me", token,
                "{\"fullName\":\"Nguyễn Văn A\",\"phone\":\"" + phone + "\",\"userType\":\"STUDENT\"}");
        assertThat(saved.statusCode()).as(saved.body()).isEqualTo(200);

        publish("auth.account.registered", "AccountRegistered",
                Map.of("accountId", accountId.toString(), "email", "early@example.com"));

        await().atMost(Duration.ofSeconds(15)).until(
                () -> "early@example.com".equals(profiles.findById(accountId).map(UserProfile::getEmail).orElse(null)));
        UserProfile profile = profiles.findById(accountId).orElseThrow();
        assertThat(profile.getFullName()).isEqualTo("Nguyễn Văn A");
        assertThat(profile.getPhone()).isEqualTo(phone);
    }

    @Test
    void malformedEventsAreDeadLetteredWithoutRetry() {
        publishRaw("auth.account.registered", bytes("{oops"));
        publish("auth.account.registered", "AccountRegistered", Map.of("email", "no-id@example.com"));
        publish("auth.account.registered", "AccountRegistered", Map.of("accountId", "not-a-uuid"));

        await().atMost(Duration.ofSeconds(15)).until(() -> queueDepth(DLQ) == 3);
    }

    @Test
    void internalSummaryReturnsTheProvisionedProfile() {
        UUID accountId = UUID.randomUUID();
        publish("auth.account.registered", "AccountRegistered",
                Map.of("accountId", accountId.toString(), "email", "summary@example.com"));
        awaitProfile(accountId);

        HttpResponse<String> summary = get("/internal/v1/users/" + accountId + "/summary", null);
        assertThat(summary.statusCode()).as(summary.body()).isEqualTo(200);
        assertThat(json(summary).get("userId").asString()).isEqualTo(accountId.toString());
        assertThat(json(summary).get("email").asString()).isEqualTo("summary@example.com");

        assertThat(get("/internal/v1/users/" + UUID.randomUUID() + "/summary", null).statusCode()).isEqualTo(404);
        assertThat(get("/internal/v1/users/not-a-uuid/summary", null).statusCode()).isEqualTo(400);
    }

    private UserProfile awaitProfile(UUID userId) {
        return await().atMost(Duration.ofSeconds(15)).until(() -> profiles.findById(userId).orElse(null), p -> p != null);
    }
}
