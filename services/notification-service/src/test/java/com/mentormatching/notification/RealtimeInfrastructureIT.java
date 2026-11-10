package com.mentormatching.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.mentormatching.notification.config.WebSocketConfiguration;
import com.mentormatching.notification.support.NotificationIntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.support.AbstractMessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/**
 * Kênh realtime STOMP chạy thật: handshake cần Bearer JWT, Principal của phiên là userId (sub),
 * và tin gửi bằng {@code convertAndSendToUser} chỉ tới đúng người nhận.
 */
class RealtimeInfrastructureIT extends NotificationIntegrationTestSupport {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    @Qualifier("brokerChannel")
    private MessageChannel brokerChannel;

    private final List<StompSession> sessions = new CopyOnWriteArrayList<>();
    private final WebSocketStompClient stompClient = newStompClient();

    @AfterEach
    void disconnect() {
        sessions.forEach(session -> {
            if (session.isConnected()) {
                session.disconnect();
            }
        });
        stompClient.stop();
    }

    @Test
    void authenticatedUserReceivesOnlyTheirOwnNotifications() throws Exception {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        BlockingQueue<Map<String, Object>> aliceInbox = subscribe(connect(KEYS.token(alice, "LEARNER"), null));
        BlockingQueue<Map<String, Object>> bobInbox = subscribe(connect(KEYS.token(bob, "MENTOR"), null));

        Map<String, Object> received = sendUntilReceived(alice, Map.of("title", "Booking confirmed"), aliceInbox);

        assertThat(received).containsEntry("title", "Booking confirmed");
        assertThat(bobInbox.poll(500, TimeUnit.MILLISECONDS)).as("other users must not receive it").isNull();
    }

    @Test
    void handshakeWithoutValidTokenIsRejected() {
        assertThatThrownBy(() -> connect(null, null)).hasStackTraceContaining("401");
        assertThatThrownBy(() -> connect("not.a.jwt", null)).hasStackTraceContaining("401");
    }

    @Test
    void handshakeFromUnknownBrowserOriginIsRejected() {
        String token = KEYS.token(UUID.randomUUID(), "LEARNER");
        assertThatThrownBy(() -> connect(token, "https://evil.example")).hasStackTraceContaining("403");
    }

    @Test
    void handshakeFromConfiguredFrontendOriginIsAccepted() throws Exception {
        StompSession session = connect(KEYS.token(UUID.randomUUID(), "LEARNER"), ALLOWED_ORIGIN);
        assertThat(session.isConnected()).isTrue();
    }

    @Test
    void messagingTemplateTargetsPerUserNotificationQueue() {
        List<Message<?>> captured = new CopyOnWriteArrayList<>();
        ChannelInterceptor capture = new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                captured.add(message);
                return message;
            }
        };
        AbstractMessageChannel channel = (AbstractMessageChannel) brokerChannel;
        channel.addInterceptor(capture);
        try {
            String userId = UUID.randomUUID().toString();
            messagingTemplate.convertAndSendToUser(userId, WebSocketConfiguration.NOTIFICATION_QUEUE, Map.of("probe", true));

            assertThat(captured).isNotEmpty();
            assertThat(SimpMessageHeaderAccessor.getDestination(captured.getFirst().getHeaders()))
                    .isEqualTo("/user/" + userId + "/queue/notifications");
        } finally {
            channel.removeInterceptor(capture);
        }
    }

    private StompSession connect(String bearerToken, String origin) throws Exception {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        if (bearerToken != null) {
            headers.add("Authorization", "Bearer " + bearerToken);
        }
        if (origin != null) {
            headers.add("Origin", origin);
        }
        String url = "ws://localhost:" + port + WebSocketConfiguration.ENDPOINT;
        StompSession session = stompClient.connectAsync(url, headers, new StompSessionHandlerAdapter() { })
                .get(10, TimeUnit.SECONDS);
        sessions.add(session);
        return session;
    }

    private static BlockingQueue<Map<String, Object>> subscribe(StompSession session) {
        BlockingQueue<Map<String, Object>> inbox = new LinkedBlockingQueue<>();
        session.subscribe(WebSocketConfiguration.USER_DESTINATION_PREFIX + WebSocketConfiguration.NOTIFICATION_QUEUE,
                new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return Map.class;
                    }

                    @Override
                    @SuppressWarnings("unchecked")
                    public void handleFrame(StompHeaders headers, Object payload) {
                        inbox.add((Map<String, Object>) payload);
                    }
                });
        return inbox;
    }

    /** SUBSCRIBE được xử lý bất đồng bộ nên gửi lặp lại tới khi nhận được (tối đa ~10 giây). */
    private Map<String, Object> sendUntilReceived(UUID userId, Map<String, Object> payload,
                                                  BlockingQueue<Map<String, Object>> inbox) throws InterruptedException {
        for (int attempt = 0; attempt < 40; attempt++) {
            messagingTemplate.convertAndSendToUser(userId.toString(), WebSocketConfiguration.NOTIFICATION_QUEUE, payload);
            Map<String, Object> received = inbox.poll(250, TimeUnit.MILLISECONDS);
            if (received != null) {
                return received;
            }
        }
        throw new AssertionError("notification was not delivered to " + userId);
    }

    private static WebSocketStompClient newStompClient() {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new JacksonJsonMessageConverter());
        return client;
    }
}
