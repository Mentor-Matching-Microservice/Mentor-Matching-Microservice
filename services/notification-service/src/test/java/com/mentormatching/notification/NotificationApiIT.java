package com.mentormatching.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mentormatching.notification.domain.Notification;
import com.mentormatching.notification.domain.NotificationType;
import com.mentormatching.notification.repository.NotificationRepository;
import com.mentormatching.notification.support.NotificationIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/**
 * REST API hộp thư (#73): chỉ đọc/sửa/xoá được thông báo của chính mình.
 */
class NotificationApiIT extends NotificationIntegrationTestSupport {

    private static final String BASE = "/api/v1/notifications";

    @Autowired
    private NotificationRepository repository;

    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();
    private String aliceToken;
    private String bobToken;
    private List<Notification> aliceNotifications;
    private Notification bobNotification;

    @BeforeEach
    void seed() {
        repository.deleteAll();
        aliceToken = KEYS.token(alice, "LEARNER");
        bobToken = KEYS.token(bob, "MENTOR");
        Instant base = Instant.parse("2026-11-01T00:00:00Z");
        aliceNotifications = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            aliceNotifications.add(Notification.create(alice, NotificationType.SYSTEM, "Title " + i, "Message " + i,
                    null, null, null, base.plusSeconds(i)));
        }
        repository.saveAll(aliceNotifications);
        bobNotification = repository.save(Notification.create(bob, NotificationType.BOOKING_CONFIRMED, "Bob", "Bob msg",
                "BOOKING", "42", null, base));
    }

    @Test
    void listIsPagedNewestFirstAndOnlyContainsOwnNotifications() {
        HttpResponse<String> response = get(BASE + "?page=1&size=5", aliceToken);

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = json(response);
        assertThat(body.get("success").asBoolean()).isTrue();
        JsonNode page = body.get("data");
        assertThat(page.get("page").asInt()).isEqualTo(1);
        assertThat(page.get("pageSize").asInt()).isEqualTo(5);
        assertThat(page.get("totalItems").asLong()).isEqualTo(12);
        assertThat(page.get("totalPages").asInt()).isEqualTo(3);
        assertThat(page.get("data")).hasSize(5);
        assertThat(page.get("data").get(0).get("title").asString()).isEqualTo("Title 11");
        assertThat(page.get("data").get(0).get("userId").asString()).isEqualTo(alice.toString());
        assertThat(page.get("data").get(0).get("isRead").asBoolean()).isFalse();
        assertThat(response.body()).doesNotContain("Bob msg");

        JsonNode last = json(get(BASE + "?page=3&size=5", aliceToken)).get("data");
        assertThat(last.get("data")).hasSize(2);
        assertThat(last.get("data").get(1).get("title").asString()).isEqualTo("Title 0");
    }

    @Test
    void invalidPagingIsRejected() {
        assertValidationError(get(BASE + "?page=0", aliceToken));
        assertValidationError(get(BASE + "?size=101", aliceToken));
        assertValidationError(get(BASE + "?size=0", aliceToken));
    }

    @Test
    void unreadCountMarkReadAndMarkAllRead() {
        assertThat(unreadCount(aliceToken)).isEqualTo(12);

        String id = aliceNotifications.getFirst().getId();
        HttpResponse<String> marked = send("PATCH", BASE + "/" + id + "/read", aliceToken, Map.of(), null);
        assertThat(marked.statusCode()).isEqualTo(200);
        JsonNode data = json(marked).get("data");
        assertThat(data.get("isRead").asBoolean()).isTrue();
        String firstReadAt = data.get("readAt").asString();
        // Đánh dấu lại không đổi thời điểm đọc đầu tiên.
        JsonNode again = json(send("PATCH", BASE + "/" + id + "/read", aliceToken, Map.of(), null)).get("data");
        assertThat(again.get("readAt").asString()).isEqualTo(firstReadAt);
        assertThat(unreadCount(aliceToken)).isEqualTo(11);

        HttpResponse<String> all = send("POST", BASE + "/mark-all-read", aliceToken, Map.of(), null);
        assertThat(all.statusCode()).isEqualTo(200);
        assertThat(json(all).get("data").asLong()).isEqualTo(11);
        assertThat(unreadCount(aliceToken)).isZero();
        assertThat(unreadCount(bobToken)).as("other users are untouched").isEqualTo(1);
    }

    @Test
    void cannotReadOrDeleteSomeoneElsesNotification() {
        String bobsId = bobNotification.getId();

        assertNotFound(send("PATCH", BASE + "/" + bobsId + "/read", aliceToken, Map.of(), null));
        assertNotFound(send("DELETE", BASE + "/" + bobsId, aliceToken, Map.of(), null));

        Notification stillThere = repository.findById(bobsId).orElseThrow();
        assertThat(stillThere.isRead()).isFalse();
    }

    @Test
    void deleteOwnNotification() {
        String id = aliceNotifications.get(3).getId();

        HttpResponse<String> deleted = send("DELETE", BASE + "/" + id, aliceToken, Map.of(), null);
        assertThat(deleted.statusCode()).isEqualTo(200);
        assertThat(repository.findById(id)).isEmpty();
        assertNotFound(send("DELETE", BASE + "/" + id, aliceToken, Map.of(), null));
    }

    @Test
    void unknownOrMalformedIds() {
        assertNotFound(send("PATCH", BASE + "/" + UUID.randomUUID() + "/read", aliceToken, Map.of(), null));
        HttpResponse<String> malformed = send("PATCH", BASE + "/not-a-uuid/read", aliceToken, Map.of(), null);
        assertThat(malformed.statusCode()).isEqualTo(400);
        assertThat(json(malformed).get("code").asString()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void everyEndpointRequiresAuthentication() {
        String id = aliceNotifications.getFirst().getId();
        assertThat(get(BASE).statusCode()).isEqualTo(401);
        assertThat(get(BASE + "/unread-count").statusCode()).isEqualTo(401);
        assertThat(send("PATCH", BASE + "/" + id + "/read", null, Map.of(), null).statusCode()).isEqualTo(401);
        assertThat(send("POST", BASE + "/mark-all-read", null, Map.of(), null).statusCode()).isEqualTo(401);
        assertThat(send("DELETE", BASE + "/" + id, null, Map.of(), null).statusCode()).isEqualTo(401);
        assertThat(repository.count()).isEqualTo(13);
    }

    private long unreadCount(String token) {
        HttpResponse<String> response = get(BASE + "/unread-count", token);
        assertThat(response.statusCode()).isEqualTo(200);
        return json(response).get("data").asLong();
    }

    private static void assertNotFound(HttpResponse<String> response) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(404);
        assertThat(json(response).get("code").asString()).isEqualTo("NOTIFICATION_NOT_FOUND");
    }

    private static void assertValidationError(HttpResponse<String> response) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(400);
        assertThat(json(response).get("code").asString()).isEqualTo("VALIDATION_ERROR");
    }
}
