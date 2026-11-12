package com.mentormatching.notification.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

class StompAuthChannelInterceptorTest {

    private static final String GOOD_TOKEN = "good.token.value";
    private final UUID userId = UUID.randomUUID();

    private final JwtDecoder decoder = token -> {
        if (!GOOD_TOKEN.equals(token)) {
            throw new BadJwtException("bad");
        }
        return Jwt.withTokenValue(token).header("alg", "RS256").subject(userId.toString())
                .claim("roles", List.of("LEARNER")).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .build();
    };
    private final StompAuthChannelInterceptor interceptor = new StompAuthChannelInterceptor(decoder, converter());

    @Test
    void connectWithValidBearerTokenSetsPrincipalToAccountId() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + GOOD_TOKEN);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        interceptor.preSend(message, null);

        assertThat(accessor.getUser()).isNotNull();
        assertThat(accessor.getUser().getName()).isEqualTo(userId.toString());
    }

    @Test
    void lowercaseHeaderNameAndSchemeAreAccepted() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("authorization", "bearer " + GOOD_TOKEN);
        accessor.setLeaveMutable(true);

        interceptor.preSend(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()), null);

        assertThat(accessor.getUser()).isNotNull();
    }

    @Test
    void connectWithoutOrWithBadTokenIsRejectedWithoutEchoingIt() {
        assertThatThrownBy(() -> interceptor.preSend(connect(null), null))
                .isInstanceOf(MessageDeliveryException.class).hasMessage("Authentication required");
        assertThatThrownBy(() -> interceptor.preSend(connect("Basic abc"), null))
                .hasMessage("Authentication required");
        assertThatThrownBy(() -> interceptor.preSend(connect("Bearer "), null))
                .hasMessage("Authentication required");
        assertThatThrownBy(() -> interceptor.preSend(connect("Bearer stolen-or-forged"), null))
                .hasMessage("Invalid token").hasMessageNotContaining("stolen-or-forged");
    }

    @Test
    void subscribeOnlyToOwnQueueAndOnlyWhenAuthenticated() {
        assertThat(interceptor.preSend(subscribe("/user/queue/notifications", true), null)).isNotNull();
        assertThatThrownBy(() -> interceptor.preSend(subscribe("/user/queue/notifications", false), null))
                .hasMessage("Authentication required");
        for (String forbidden : List.of("/queue/notifications-userabc", "/topic/all", "/user/queue/other", "/app/x")) {
            assertThatThrownBy(() -> interceptor.preSend(subscribe(forbidden, true), null))
                    .as(forbidden).hasMessage("Subscription destination is not allowed");
        }
    }

    @Test
    void sendIsNeverAllowed() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/notifications");
        accessor.setUser(new TestingAuthenticationToken(userId.toString(), null));
        assertThatThrownBy(() -> interceptor.preSend(
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()), null))
                .hasMessage("Sending messages is not supported");
    }

    @Test
    void otherFramesPassThrough() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        assertThat(interceptor.preSend(message, null)).isSameAs(message);
    }

    private static Message<byte[]> connect(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> subscribe(String destination, boolean authenticated) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        if (authenticated) {
            accessor.setUser(new TestingAuthenticationToken(userId.toString(), null));
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private static JwtAuthenticationConverter converter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }
}
