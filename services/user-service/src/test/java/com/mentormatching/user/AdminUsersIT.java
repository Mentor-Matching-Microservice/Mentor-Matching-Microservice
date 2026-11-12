package com.mentormatching.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import com.mentormatching.user.entity.UserProfile;
import com.mentormatching.user.entity.UserType;
import com.mentormatching.user.repository.UserProfileRepository;
import com.mentormatching.user.support.UserIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/**
 * #66 trên MySQL + RabbitMQ thật (auth-service giả lập): API admin, bản sao trạng thái tài khoản, API nội bộ.
 */
class AdminUsersIT extends UserIntegrationTestSupport {

    private static final String BASE = "/api/v1/admin/users";

    @Autowired private UserProfileRepository profiles;

    private final String adminToken = KEYS.token(UUID.randomUUID(), "ADMIN");
    private final String marker = "zq" + UUID.randomUUID().toString().substring(0, 6);
    private UUID alice;
    private UUID bob;
    private UUID carol;

    @BeforeEach
    void seed() {
        alice = save("Trần Thị " + marker.toUpperCase() + " Alice", "0911" + digits(), UserType.STUDENT);
        bob = save("Lê Văn " + marker + " Bob", "0922" + digits(), UserType.PARENT);
        carol = save("Phạm Carol", "0933" + digits(), UserType.STUDENT);
    }

    @Test
    void adminSearchesByNameOrPhoneCaseInsensitivelyWithPaging() {
        JsonNode page = data(get(BASE + "?q=" + marker + "&page=1&size=1", adminToken));
        assertThat(page.get("totalItems").asLong()).isEqualTo(2);
        assertThat(page.get("totalPages").asInt()).isEqualTo(2);
        assertThat(page.get("data")).hasSize(1);
        assertThat(page.get("data").get(0).get("userId").asString()).isEqualTo(bob.toString()); // mới nhất trước

        String carolPhone = profiles.findById(carol).orElseThrow().getPhone();
        JsonNode byPhone = data(get(BASE + "?q=" + carolPhone.substring(3), adminToken));
        assertThat(byPhone.get("data")).anySatisfy(u -> assertThat(u.get("userId").asString()).isEqualTo(carol.toString()));

        JsonNode students = data(get(BASE + "?q=" + marker + "&userType=STUDENT", adminToken));
        assertThat(students.get("totalItems").asLong()).isEqualTo(1);
        assertThat(students.get("data").get(0).get("userId").asString()).isEqualTo(alice.toString());

        // % và _ trong từ khoá là ký tự thường, không phải wildcard.
        assertThat(data(get(BASE + "?q=%25", adminToken)).get("totalItems").asLong()).isZero();
    }

    @Test
    void adminSeesTheDetailOfOneUser() {
        JsonNode detail = data(get(BASE + "/" + alice, adminToken));
        assertThat(detail.get("fullName").asString()).contains("Alice");
        assertThat(detail.get("userType").asString()).isEqualTo("STUDENT");
        assertThat(detail.get("learnerProfile").isNull()).isTrue();
        assertProblem(get(BASE + "/" + UUID.randomUUID(), adminToken), 404, "USER_PROFILE_NOT_FOUND");
    }

    @Test
    void onlyAdminsMayUseTheAdminApi() {
        assertProblem(get(BASE, KEYS.token(UUID.randomUUID(), "LEANER")), 403, "ACCESS_DENIED");
        assertProblem(get(BASE + "/" + alice, KEYS.token(UUID.randomUUID(), "MENTOR")), 403, "ACCESS_DENIED");
        assertProblem(send("PATCH", BASE + "/" + alice + "/status", KEYS.token(UUID.randomUUID(), "LEANER"),
                "{\"status\":\"LOCKED\"}"), 403, "ACCESS_DENIED");
        assertProblem(get(BASE, null), 401, "AUTHENTICATION_REQUIRED");
        assertThat(get(BASE + "?size=101", adminToken).statusCode()).isEqualTo(400);
        assertThat(get(BASE + "?page=0", adminToken).statusCode()).isEqualTo(400);
        assertThat(get(BASE + "?userType=ROBOT", adminToken).statusCode()).isEqualTo(400);
    }

    @Test
    void statusChangeIsDelegatedToAuthServiceWithTheAdminsToken() {
        HttpResponse<String> changed = send("PATCH", BASE + "/" + alice + "/status", adminToken,
                "{\"status\":\"LOCKED\",\"reason\":\"Spam\"}");
        assertThat(changed.statusCode()).as(changed.body()).isEqualTo(200);
        assertThat(json(changed).get("data").get("status").asString()).isEqualTo("LOCKED");
        assertThat(AUTH.authorizations()).contains("Bearer " + adminToken);
        assertThat(profiles.findById(alice).orElseThrow().getAccountStatus()).isEqualTo("LOCKED");

        assertThat(send("PATCH", BASE + "/" + alice + "/status", adminToken, "{\"status\":\"PENDING_VERIFICATION\"}")
                .statusCode()).isEqualTo(400);
        AUTH.failWith(bob.toString(), 404);
        assertProblem(send("PATCH", BASE + "/" + bob + "/status", adminToken, "{\"status\":\"LOCKED\"}"), 404,
                "RESOURCE_NOT_FOUND");
        AUTH.failWith(carol.toString(), 500);
        assertProblem(send("PATCH", BASE + "/" + carol + "/status", adminToken, "{\"status\":\"LOCKED\"}"), 503,
                "SERVICE_UNAVAILABLE");
        assertThat(profiles.findById(carol).orElseThrow().getAccountStatus()).isNull();
    }

    @Test
    void accountStatusEventsKeepTheProjectionInOrder() {
        UUID user = UUID.randomUUID();
        publish("auth.account.registered", "AccountRegistered", Map.of("accountId", user.toString(),
                "email", "status@example.com", "status", "PENDING_VERIFICATION"));
        await().atMost(Duration.ofSeconds(15)).until(() -> "PENDING_VERIFICATION".equals(status(user)));

        publish("auth.account.status-changed", "AccountStatusChanged", Map.of("accountId", user.toString(),
                "status", "LOCKED", "previousStatus", "ACTIVE", "aggregateVersion", 5));
        await().atMost(Duration.ofSeconds(15)).until(() -> "LOCKED".equals(status(user)));

        // Sự kiện cũ (version nhỏ hơn) tới muộn: bị bỏ qua.
        publish("auth.account.status-changed", "AccountStatusChanged", Map.of("accountId", user.toString(),
                "status", "ACTIVE", "previousStatus", "PENDING_VERIFICATION", "aggregateVersion", 3));
        await().pollDelay(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(10))
                .until(() -> queueDepth("user.account-events") == 0);
        assertThat(status(user)).isEqualTo("LOCKED");
        assertThat(profiles.findById(user).orElseThrow().getAccountStatusVersion()).isEqualTo(5);

        JsonNode list = data(get(BASE + "?q=", adminToken));
        assertThat(list.get("data").toString()).contains("LOCKED");
    }

    @Test
    void internalApiReturnsSummariesSingleAndInBatch() {
        JsonNode one = json(get("/internal/v1/users/" + alice, null));
        assertThat(one.get("userId").asString()).isEqualTo(alice.toString());
        assertThat(one.get("userType").asString()).isEqualTo("STUDENT");
        assertThat(json(get("/internal/v1/users/" + alice + "/summary", null)).get("fullName").asString()).contains("Alice");

        HttpResponse<String> batch = send("POST", "/internal/v1/users/batch-summary", null,
                "{\"userIds\":[\"" + alice + "\",\"" + bob + "\",\"" + alice + "\",\"" + UUID.randomUUID() + "\"]}");
        assertThat(batch.statusCode()).as(batch.body()).isEqualTo(200);
        assertThat(json(batch)).hasSize(2);
        assertThat(send("POST", "/internal/v1/users/batch-summary", null, "{\"userIds\":[]}").statusCode()).isEqualTo(400);
    }

    private UUID save(String name, String phone, UserType type) {
        UserProfile p = new UserProfile();
        UUID id = UUID.randomUUID();
        p.setUserId(id);
        p.setFullName(name);
        p.setPhone(phone);
        p.setUserType(type);
        profiles.saveAndFlush(p);
        sleepMillis(5); // created_at khác nhau để thứ tự "mới nhất trước" ổn định
        return id;
    }

    private String status(UUID user) {
        return profiles.findById(user).map(UserProfile::getAccountStatus).orElse(null);
    }

    private static String digits() {
        return String.valueOf(System.nanoTime()).substring(7, 13);
    }

    private static void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static JsonNode data(HttpResponse<String> response) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        return json(response).get("data");
    }

    private static void assertProblem(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        assertThat(json(response).get("code").asString()).isEqualTo(code);
    }
}
