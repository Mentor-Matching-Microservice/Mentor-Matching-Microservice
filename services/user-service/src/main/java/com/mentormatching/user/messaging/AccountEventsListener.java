package com.mentormatching.user.messaging;

import java.util.UUID;

import com.mentormatching.user.service.AccountStatusProjectionUseCase;
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
 * Nhận sự kiện tài khoản từ auth-service. {@code AccountStatusChanged} (#66) cập nhật bản sao trạng thái tài khoản.
 * {@code AccountRegistered} (routing {@code auth.account.registered}):
 * trường bắt buộc {@code eventId}, {@code eventType}, {@code accountId} (UUID = {@code sub} JWT); tuỳ chọn {@code email}.
 * Loại sự kiện khác được bỏ qua. Log chỉ ghi id/loại sự kiện, không ghi email.
 */
@Component
@ConditionalOnProperty(name = "user.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class AccountEventsListener {

    public static final String ACCOUNT_REGISTERED = "AccountRegistered";
    public static final String ACCOUNT_STATUS_CHANGED = "AccountStatusChanged";
    static final java.util.Set<String> STATUSES = java.util.Set.of("PENDING_VERIFICATION", "ACTIVE", "LOCKED", "DISABLED");
    private static final Logger log = LoggerFactory.getLogger(AccountEventsListener.class);

    private final ObjectMapper objectMapper;
    private final ProvisionUserProfileUseCase provision;
    private final AccountStatusProjectionUseCase statusProjection;

    public AccountEventsListener(ObjectMapper objectMapper, ProvisionUserProfileUseCase provision,
                                 AccountStatusProjectionUseCase statusProjection) {
        this.objectMapper = objectMapper;
        this.provision = provision;
        this.statusProjection = statusProjection;
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
        if (ACCOUNT_STATUS_CHANGED.equals(eventType)) {
            UUID accountId = uuid(event, "accountId");
            String status = status(event, "status");
            JsonNode version = event.get("aggregateVersion");
            if (version == null || !version.canConvertToLong()) {
                throw new InvalidEventException("Missing required field aggregateVersion");
            }
            var outcome = statusProjection.apply(eventId, eventType, accountId, status, version.asLong());
            log.info("AccountStatusChanged eventId={} userId={} outcome={}", eventId, accountId, outcome);
            return;
        }
        if (!ACCOUNT_REGISTERED.equals(eventType)) {
            return;
        }
        UUID accountId = uuid(event, "accountId");
        String initialStatus = event.has("status") ? status(event, "status") : null;
        JsonNode email = event.get("email");
        String emailValue = email != null && email.isString() && !email.asString().isBlank() ? email.asString() : null;
        if (emailValue != null && emailValue.length() > 254) {
            throw new InvalidEventException("email is too long");
        }
        var outcome = provision.provision(eventId, eventType, accountId, emailValue, initialStatus);
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

    private static String status(JsonNode node, String field) {
        String value = text(node, field);
        if (!STATUSES.contains(value)) {
            throw new InvalidEventException(field + " is not a known account status");
        }
        return value;
    }

    private static UUID uuid(JsonNode node, String field) {
        try {
            return UUID.fromString(text(node, field));
        } catch (IllegalArgumentException ex) {
            throw new InvalidEventException(field + " must be a UUID");
        }
    }
}
