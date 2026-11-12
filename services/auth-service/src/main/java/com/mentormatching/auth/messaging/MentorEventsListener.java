package com.mentormatching.auth.messaging;

import java.time.Instant;
import java.util.UUID;

import com.mentormatching.auth.entity.InboxEvent;
import com.mentormatching.auth.exception.ApiException;
import com.mentormatching.auth.repository.InboxEventRepository;
import com.mentormatching.auth.service.GrantMentorRoleUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Nhận {@code MentorApproved} (mentor-service, #69) và gán role {@code MENTOR} cho tài khoản, idempotent:
 * bản ghi {@code inbox_events} (khoá chính = {@code eventId}) và việc gán role nằm trong cùng một transaction,
 * nên nhận trùng sự kiện không lỗi và không nhân đôi role. Các loại sự kiện khác trên {@code mentor.#} được bỏ qua.
 *
 * <p>Trường bắt buộc: {@code eventId}, {@code eventType}, {@code mentorUserId} (hoặc {@code userId}/{@code accountId}) là UUID.
 */
@Component
@ConditionalOnProperty(name = "auth.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class MentorEventsListener {

    public static final String MENTOR_APPROVED = "MentorApproved";
    private static final Logger log = LoggerFactory.getLogger(MentorEventsListener.class);

    private final ObjectMapper objectMapper;
    private final InboxEventRepository inbox;
    private final GrantMentorRoleUseCase grantMentorRole;
    private final TransactionTemplate transaction;

    public MentorEventsListener(ObjectMapper objectMapper, InboxEventRepository inbox,
                                GrantMentorRoleUseCase grantMentorRole, PlatformTransactionManager transactionManager) {
        this.objectMapper = objectMapper;
        this.inbox = inbox;
        this.grantMentorRole = grantMentorRole;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @RabbitListener(queues = "${auth.messaging.queue:auth.mentor-events}",
            containerFactory = AuthMessagingConfiguration.LISTENER_CONTAINER_FACTORY)
    public void onMessage(Message message) {
        JsonNode event = parse(message.getBody());
        String eventId = text(event, "eventId");
        String eventType = text(event, "eventType");
        if (!MENTOR_APPROVED.equals(eventType)) {
            return; // mentor.rating, ... không liên quan tới auth
        }
        if (eventId.length() > 100) {
            throw new InvalidEventException("eventId is too long");
        }
        UUID accountId = accountId(event);
        try {
            Boolean granted = transaction.execute(status -> {
                if (inbox.existsById(eventId)) {
                    return null;
                }
                inbox.saveAndFlush(new InboxEvent(eventId, eventType, Instant.now()));
                return grantMentorRole.execute(accountId);
            });
            log.info("MentorApproved eventId={} accountId={} outcome={}", eventId, accountId,
                    granted == null ? "DUPLICATE" : granted ? "GRANTED" : "ALREADY_MENTOR");
        } catch (DataIntegrityViolationException duplicate) {
            log.info("MentorApproved eventId={} outcome=DUPLICATE", eventId); // lần giao song song đã ghi inbox
        } catch (ApiException notFound) {
            throw new InvalidEventException("Account not found"); // tài khoản không tồn tại: không thử lại
        }
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

    private static UUID accountId(JsonNode event) {
        for (String field : new String[] {"mentorUserId", "userId", "accountId"}) {
            JsonNode value = event.get(field);
            if (value != null && value.isString() && !value.asString().isBlank()) {
                try {
                    return UUID.fromString(value.asString().strip());
                } catch (IllegalArgumentException ex) {
                    throw new InvalidEventException(field + " must be a UUID");
                }
            }
        }
        throw new InvalidEventException("Missing required field mentorUserId");
    }
}
