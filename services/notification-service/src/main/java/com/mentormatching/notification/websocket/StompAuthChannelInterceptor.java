package com.mentormatching.notification.websocket;

import java.util.UUID;

import com.mentormatching.notification.config.WebSocketConfiguration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

/**
 * Xác thực STOMP ở frame CONNECT (trình duyệt không gửi được header Authorization khi handshake WebSocket):
 * <ul>
 *   <li>CONNECT phải có native header {@code Authorization: Bearer <JWT RS256>} hợp lệ; nếu thiếu/sai,
 *       server trả frame ERROR và đóng kết nối. Principal của phiên = {@code sub} (accountId).</li>
 *   <li>SUBSCRIBE chỉ được tới {@code /user/queue/notifications} (hàng đợi của chính mình).</li>
 *   <li>SEND bị chặn: kênh này chỉ để server đẩy xuống.</li>
 * </ul>
 * Thông báo lỗi là chuỗi cố định, không bao giờ chứa token.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    static final String ALLOWED_SUBSCRIPTION =
            WebSocketConfiguration.USER_DESTINATION_PREFIX + WebSocketConfiguration.NOTIFICATION_QUEUE;

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter authenticationConverter;

    public StompAuthChannelInterceptor(JwtDecoder jwtDecoder, JwtAuthenticationConverter authenticationConverter) {
        this.jwtDecoder = jwtDecoder;
        this.authenticationConverter = authenticationConverter;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        switch (accessor.getCommand()) {
            case CONNECT, STOMP -> accessor.setUser(authenticate(accessor));
            case SUBSCRIBE -> {
                requireAuthenticated(accessor);
                if (!ALLOWED_SUBSCRIPTION.equals(accessor.getDestination())) {
                    throw new MessageDeliveryException("Subscription destination is not allowed");
                }
            }
            case SEND -> throw new MessageDeliveryException("Sending messages is not supported");
            default -> {
                // DISCONNECT, UNSUBSCRIBE, ACK, NACK...: không cần kiểm tra thêm.
            }
        }
        return message;
    }

    private AbstractAuthenticationToken authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null) {
            header = accessor.getFirstNativeHeader("authorization");
        }
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7) || header.length() <= 7) {
            throw new MessageDeliveryException("Authentication required");
        }
        try {
            Jwt jwt = jwtDecoder.decode(header.substring(7).strip());
            AbstractAuthenticationToken authentication = authenticationConverter.convert(jwt);
            if (authentication == null) {
                throw new MessageDeliveryException("Invalid token");
            }
            UUID.fromString(authentication.getName()); // JwtConfiguration đã bảo đảm sub là UUID
            return authentication;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new MessageDeliveryException("Invalid token");
        }
    }

    private static void requireAuthenticated(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            throw new MessageDeliveryException("Authentication required");
        }
    }
}
