package com.mentormatching.user.messaging;

import java.util.UUID;

import com.mentormatching.user.service.ProvisionUserProfileUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Nhận sự kiện tài khoản từ auth-service. {@code AccountRegistered} (routing {@code auth.account.registered}):
 * trường bắt buộc {@code eventId}, {@code eventType}, {@code accountId} (UUID = {@code sub} JWT); tuỳ chọn {@code email}.
 * Loại sự kiện khác được bỏ qua. Log chỉ ghi id/loại sự kiện, không ghi email.
 */
@Component
@ConditionalOnProperty(name = "user.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class AccountEventsListener {

    public static final String ACCOUNT_REGISTERED = "AccountRegistered";
    private static final Logger log = LoggerFactory.getLogger(AccountEventsListener.class);

    private final ObjectMapper objectMapper;
    private final ProvisionUserProfileUseCase provision;

    public AccountEventsListener(ObjectMapper objectMapper, ProvisionUserProfileUseCase provision) {
        this.objectMapper = objectMapper;
        this.provision = provision;
    }

    @RabbitListener(queues = "${user.messaging.queue:user.account-events}",
            containerFactory = UserMessagingConfiguration.LISTENER_CONTAINER_FACTORY)
    public void onMessage(Message message) {
        JsonNode event = parse(message.getBody());
        String eventId = text(event, "eventId");
        String eventType = text(event, "eventType");
        if (eventId.length() > 100) {
            throw new InvalidEventException("eventId is too long");
        }
        if (!ACCOUNT_REGISTERED.equals(eventType)) {
            return;
        }
        UUID accountId = uuid(event, "accountId");
        JsonNode email = event.get("email");
        String emailValue = email != null && email.isString() && !email.asString().isBlank() ? email.asString() : null;
        if (emailValue != null && emailValue.length() > 254) {
            throw new InvalidEventException("email is too long");
        }
        var outcome = provision.provision(eventId, eventType, accountId, emailValue);
        log.info("AccountRegistered eventId={} userId={} outcome={}", eventId, accountId, outcome);
    }

    private JsonNode parse(byte[] body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node == null || !node.isObject()) {
                throw new InvalidEventException("Event body must be a JSON object");
            }
            return node;
        } catch (JacksonException ex) {
            throw new InvalidEventException("Event body is not valid JSON");
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isString() || value.asString().isBlank()) {
            throw new InvalidEventException("Missing required field " + field);
        }
        return value.asString().strip();
    }

    private static UUID uuid(JsonNode node, String field) {
        try {
            return UUID.fromString(text(node, field));
        } catch (IllegalArgumentException ex) {
            throw new InvalidEventException(field + " must be a UUID");
        }
    }
}
