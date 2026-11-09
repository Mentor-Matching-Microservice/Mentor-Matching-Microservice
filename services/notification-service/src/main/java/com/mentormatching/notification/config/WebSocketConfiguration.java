package com.mentormatching.notification.config;

import java.util.List;

import com.mentormatching.notification.websocket.StompAuthChannelInterceptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP để đẩy thông báo realtime theo từng người dùng.
 *
 * <ul>
 *   <li>Endpoint: {@code /ws/notifications} (WebSocket thuần, không SockJS). Handshake chỉ kiểm tra origin;
 *       xác thực nằm ở frame CONNECT ({@link StompAuthChannelInterceptor}).</li>
 *   <li>Client đăng ký {@code /user/queue/notifications}; service gửi bằng
 *       {@code SimpMessagingTemplate.convertAndSendToUser(userId, NOTIFICATION_QUEUE, payload)}.</li>
 *   <li>Heartbeat 10 giây hai chiều để phát hiện kết nối chết (client cũ đề nghị 4 giây; STOMP lấy giá trị lớn hơn).</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSocketMessageBroker
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

    public static final String ENDPOINT = "/ws/notifications";
    public static final String USER_DESTINATION_PREFIX = "/user";
    public static final String NOTIFICATION_QUEUE = "/queue/notifications";
    static final long HEARTBEAT_MILLIS = 10_000;

    private final List<String> allowedOrigins;
    private final StompAuthChannelInterceptor authInterceptor;
    private final TaskScheduler heartbeatScheduler;

    public WebSocketConfiguration(
            @Value("${notification.cors.allowed-origins:http://localhost:5173}") List<String> allowedOrigins,
            @Lazy StompAuthChannelInterceptor authInterceptor,
            @Lazy @Qualifier("messageBrokerTaskScheduler") TaskScheduler heartbeatScheduler) {
        this.allowedOrigins = List.copyOf(allowedOrigins);
        this.authInterceptor = authInterceptor;
        this.heartbeatScheduler = heartbeatScheduler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Chỉ origin frontend đã cấu hình được mở handshake từ trình duyệt (chống Cross-Site WebSocket Hijacking).
        registry.addEndpoint(ENDPOINT).setAllowedOrigins(allowedOrigins.toArray(String[]::new));
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // messageBrokerTaskScheduler do Spring WebSocket tạo và quản lý vòng đời (đóng khi tắt ứng dụng).
        registry.enableSimpleBroker("/queue")
                .setHeartbeatValue(new long[] {HEARTBEAT_MILLIS, HEARTBEAT_MILLIS})
                .setTaskScheduler(heartbeatScheduler);
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix(USER_DESTINATION_PREFIX);
    }
}
