package com.mentormatching.notification.support;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.mentormatching.notification.config.WebSocketConfiguration;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/**
 * STOMP client giống frontend: handshake không có header Authorization, token gửi trong frame CONNECT.
 */
public final class StompTestClient implements AutoCloseable {

    private final WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
    private final List<StompSession> sessions = new CopyOnWriteArrayList<>();
    private final int port;

    public StompTestClient(int port) {
        this.port = port;
        client.setMessageConverter(new JacksonJsonMessageConverter());
    }

    /** Kết nối; {@code bearerToken} null nghĩa là CONNECT không có header Authorization. */
    public StompSession connect(String bearerToken, String origin) throws Exception {
        WebSocketHttpHeaders handshake = new WebSocketHttpHeaders();
        if (origin != null) {
            handshake.add("Origin", origin);
        }
        StompHeaders connect = new StompHeaders();
        if (bearerToken != null) {
            connect.add("Authorization", "Bearer " + bearerToken);
        }
        String url = "ws://localhost:" + port + WebSocketConfiguration.ENDPOINT;
        StompSession session = client.connectAsync(url, handshake, connect, new StompSessionHandlerAdapter() {
            @Override
            public void handleException(StompSession s, StompCommand command, StompHeaders headers, byte[] payload,
                                        Throwable exception) {
                // Lỗi xử lý frame trong test được thể hiện qua việc không nhận được message.
            }
        }).get(10, TimeUnit.SECONDS);
        sessions.add(session);
        return session;
    }

    /** Đăng ký hàng đợi thông báo của chính người dùng; trả về hộp nhận. */
    public static BlockingQueue<Map<String, Object>> subscribeOwnQueue(StompSession session) {
        return subscribe(session, WebSocketConfiguration.USER_DESTINATION_PREFIX + WebSocketConfiguration.NOTIFICATION_QUEUE);
    }

    public static BlockingQueue<Map<String, Object>> subscribe(StompSession session, String destination) {
        BlockingQueue<Map<String, Object>> inbox = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
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

    @Override
    public void close() {
        sessions.forEach(session -> {
            if (session.isConnected()) {
                session.disconnect();
            }
        });
        client.stop();
    }
}
