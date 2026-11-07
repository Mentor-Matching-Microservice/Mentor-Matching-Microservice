package com.mentormatching.notification.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Hạ tầng STOMP để đẩy thông báo realtime theo từng người dùng.
 *
 * <ul>
 *   <li>Endpoint: {@code /ws/notifications} (WebSocket thuần, không SockJS).</li>
 *   <li>Client đăng ký {@code /user/queue/notifications}; service gửi bằng
 *       {@code SimpMessagingTemplate.convertAndSendToUser(userId, NOTIFICATION_QUEUE, payload)}.</li>
 *   <li>Xác thực ở bước khởi tạo (#62): handshake đi qua cùng filter Bearer JWT RS256 của HTTP,
 *       nên Principal của phiên chính là {@code sub} (accountId). Handshake thiếu/sai token bị chặn 401.</li>
 * </ul>
 *
 * <p>#73 sẽ chuyển việc xác thực sang channel interceptor kiểm tra JWT trong frame STOMP CONNECT
 * (trình duyệt không gửi được header Authorization khi handshake) và cho handshake đi qua.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSocketMessageBroker
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

    public static final String ENDPOINT = "/ws/notifications";
    public static final String USER_DESTINATION_PREFIX = "/user";
    public static final String NOTIFICATION_QUEUE = "/queue/notifications";

    private final List<String> allowedOrigins;

    public WebSocketConfiguration(
            @Value("${notification.cors.allowed-origins:http://localhost:5173}") List<String> allowedOrigins) {
        this.allowedOrigins = List.copyOf(allowedOrigins);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Chỉ origin frontend đã cấu hình được mở handshake từ trình duyệt (chống Cross-Site WebSocket Hijacking).
        registry.addEndpoint(ENDPOINT).setAllowedOrigins(allowedOrigins.toArray(String[]::new));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix(USER_DESTINATION_PREFIX);
    }
}
