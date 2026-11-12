package com.mentormatching.auth.messaging;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mentormatching.auth.entity.Account;
import com.mentormatching.auth.entity.AccountStatus;
import com.mentormatching.auth.entity.OutboxEvent;
import com.mentormatching.auth.repository.OutboxEventRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Ghi sự kiện tài khoản vào outbox, trong CÙNG transaction với thay đổi dữ liệu (bắt buộc có transaction).
 *
 * <p>Định dạng phẳng giống các service khác: {@code {eventId, eventType, occurredAt, ...}}.
 * <ul>
 *   <li>{@code AccountRegistered} (routing {@code auth.account.registered}):
 *       {@code accountId, email, status, roles} — notification gửi thư chào mừng, user-service tạo hồ sơ.</li>
 *   <li>{@code AccountStatusChanged} (routing {@code auth.account.status-changed}):
 *       {@code accountId, status, previousStatus, aggregateVersion, reason} — {@code aggregateVersion} tăng dần
 *       theo mỗi lần đổi, để consumer bỏ qua sự kiện cũ tới muộn.</li>
 * </ul>
 */
@Component
public class AccountEvents {

    public static final String ACCOUNT_REGISTERED = "AccountRegistered";
    public static final String ACCOUNT_STATUS_CHANGED = "AccountStatusChanged";
    public static final String ROUTING_REGISTERED = "auth.account.registered";
    public static final String ROUTING_STATUS_CHANGED = "auth.account.status-changed";
    static final String AGGREGATE_TYPE = "Account";

    private final OutboxEventRepository outbox;
    private final ObjectMapper objectMapper;

    public AccountEvents(OutboxEventRepository outbox, ObjectMapper objectMapper) {
        this.outbox = outbox;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public UUID accountRegistered(Account account, List<String> roles) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("accountId", account.getId().toString());
        fields.put("email", account.getLoginEmail());
        fields.put("status", account.getStatus().name());
        fields.put("roles", List.copyOf(roles));
        return record(ACCOUNT_REGISTERED, ROUTING_REGISTERED, account.getId(), fields);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public UUID accountStatusChanged(Account account, AccountStatus previous, long aggregateVersion, String reason) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("accountId", account.getId().toString());
        fields.put("status", account.getStatus().name());
        fields.put("previousStatus", previous.name());
        fields.put("aggregateVersion", aggregateVersion);
        if (reason != null) {
            fields.put("reason", reason);
        }
        return record(ACCOUNT_STATUS_CHANGED, ROUTING_STATUS_CHANGED, account.getId(), fields);
    }

    private UUID record(String eventType, String routingKey, UUID aggregateId, Map<String, Object> fields) {
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", eventId.toString());
        payload.put("eventType", eventType);
        payload.put("occurredAt", now.toString());
        payload.putAll(fields);
        outbox.save(new OutboxEvent(eventId, AGGREGATE_TYPE, aggregateId, eventType, routingKey,
                objectMapper.writeValueAsString(payload), now));
        return eventId;
    }
}
