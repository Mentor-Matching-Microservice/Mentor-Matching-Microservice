package com.mentormatching.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.mentormatching.notification.config.WebSocketConfiguration;
import com.mentormatching.notification.support.NotificationIntegrationTestSupport;
import com.mentormatching.notification.support.StompTestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompSession;

/**
 * Kênh STOMP như frontend dùng: handshake không token, xác thực ở frame CONNECT (#73).
 */
class StompRealtimeIT extends NotificationIntegrationTestSupport {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private StompTestClient stomp;

    @AfterEach
    void close() {
        if (stomp != null) {
            stomp.close();
        }
    }

    @Test
    void connectWithValidTokenReceivesOnlyOwnNotifications() throws Exception {
        stomp = new StompTestClient(port);
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        BlockingQueue<Map<String, Object>> aliceInbox =
                StompTestClient.subscribeOwnQueue(stomp.connect(KEYS.token(alice, "LEARNER"), ALLOWED_ORIGIN));
        BlockingQueue<Map<String, Object>> bobInbox =
                StompTestClient.subscribeOwnQueue(stomp.connect(KEYS.token(bob, "MENTOR"), null));

        Map<String, Object> received = sendUntilReceived(alice, aliceInbox);

        assertThat(received).containsEntry("title", "probe");
        assertThat(bobInbox.poll(500, TimeUnit.MILLISECONDS)).as("other users must not receive it").isNull();
    }

    @Test
    void connectWithoutTokenIsRejected() {
        stomp = new StompTestClient(port);
        assertThatThrownBy(() -> stomp.connect(null, ALLOWED_ORIGIN)).isInstanceOf(Exception.class);
    }

    @Test
    void connectWithInvalidOrForeignTokenIsRejected() {
        stomp = new StompTestClient(port);
        assertThatThrownBy(() -> stomp.connect("not.a.jwt", null)).isInstanceOf(Exception.class);
        String wrongAudience = KEYS.token(c -> c.audience(java.util.List.of("other-api")));
        assertThatThrownBy(() -> stomp.connect(wrongAudience, null)).isInstanceOf(Exception.class);
        String otherKey = com.mentormatching.notification.support.TestJwtKeys.untrusted(2048)
                .token(UUID.randomUUID(), "LEARNER");
        assertThatThrownBy(() -> stomp.connect(otherKey, null)).isInstanceOf(Exception.class);
    }

    @Test
    void subscribingToAnythingButOwnQueueClosesTheSession() throws Exception {
        stomp = new StompTestClient(port);
        StompSession session = stomp.connect(KEYS.token(UUID.randomUUID(), "LEARNER"), null);

        StompTestClient.subscribe(session, "/queue/notifications-user" + UUID.randomUUID());

        await().atMost(Duration.ofSeconds(5)).until(() -> !session.isConnected());
    }

    @Test
    void sendingFramesIsNotAllowed() throws Exception {
        stomp = new StompTestClient(port);
        StompSession session = stomp.connect(KEYS.token(UUID.randomUUID(), "LEARNER"), null);

        session.send("/app/anything", Map.of("x", 1));

        await().atMost(Duration.ofSeconds(5)).until(() -> !session.isConnected());
    }

    @Test
    void handshakeFromUnknownBrowserOriginIsRejected() {
        stomp = new StompTestClient(port);
        String token = KEYS.token(UUID.randomUUID(), "LEARNER");
        assertThatThrownBy(() -> stomp.connect(token, "https://evil.example")).hasStackTraceContaining("403");
    }

    @Test
    void plainHttpGetOnTheEndpointIsNotAnApi() {
        // Handshake được cho qua ở HTTP để xác thực ở CONNECT; GET thường không phải upgrade nên bị từ chối.
        assertThat(get(WebSocketConfiguration.ENDPOINT).statusCode()).isIn(400, 426);
    }

    private Map<String, Object> sendUntilReceived(UUID userId, BlockingQueue<Map<String, Object>> inbox)
            throws InterruptedException {
        for (int attempt = 0; attempt < 40; attempt++) {
            messagingTemplate.convertAndSendToUser(userId.toString(), WebSocketConfiguration.NOTIFICATION_QUEUE,
                    Map.of("title", "probe"));
            Map<String, Object> received = inbox.poll(250, TimeUnit.MILLISECONDS);
            if (received != null) {
                return received;
            }
        }
        throw new AssertionError("notification was not delivered to " + userId);
    }
}
