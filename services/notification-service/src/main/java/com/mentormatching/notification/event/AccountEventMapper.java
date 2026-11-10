package com.mentormatching.notification.event;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mentormatching.notification.domain.NotificationType;
import org.springframework.stereotype.Component;

/**
 * {@code AccountRegistered} (auth-service, #64) → thông báo chào mừng cho chính tài khoản vừa tạo.
 * Trường bắt buộc: {@code accountId} (hoặc {@code userId}) là UUID = {@code sub} trong JWT.
 */
@Component
public class AccountEventMapper implements EventNotificationMapper {

    public static final String ACCOUNT_REGISTERED = "AccountRegistered";

    @Override
    public Set<String> eventTypes() {
        return Set.of(ACCOUNT_REGISTERED);
    }

    @Override
    public List<NotificationDraft> map(DomainEvent event) {
        UUID accountId = event.requiredUuid("accountId", "userId");
        return List.of(new NotificationDraft(accountId, NotificationType.ACCOUNT_REGISTERED,
                "Chào mừng bạn đến với Mentor Matching",
                "Tài khoản của bạn đã được tạo thành công. Hãy hoàn thiện hồ sơ để bắt đầu tìm mentor phù hợp.",
                "ACCOUNT", accountId.toString()));
    }
}
