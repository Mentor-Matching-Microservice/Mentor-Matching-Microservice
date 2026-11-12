package com.mentormatching.notification.event;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mentormatching.notification.domain.NotificationType;
import org.springframework.stereotype.Component;

/**
 * {@code MentorApproved} / {@code MentorRejected} (mentor-service, #69) → thông báo cho người nộp hồ sơ mentor.
 * Trường bắt buộc: {@code mentorUserId} (hoặc {@code userId}) là UUID tài khoản; tuỳ chọn: {@code mentorId}, {@code reason}.
 */
@Component
public class MentorVerificationEventMapper implements EventNotificationMapper {

    public static final String MENTOR_APPROVED = "MentorApproved";
    public static final String MENTOR_REJECTED = "MentorRejected";
    static final int MAX_REASON_LENGTH = 500;

    @Override
    public Set<String> eventTypes() {
        return Set.of(MENTOR_APPROVED, MENTOR_REJECTED);
    }

    @Override
    public List<NotificationDraft> map(DomainEvent event) {
        UUID userId = event.requiredUuid("mentorUserId", "userId");
        String mentorId = event.optionalText("mentorId");
        String reference = mentorId != null ? mentorId : userId.toString();
        if (MENTOR_APPROVED.equals(event.eventType())) {
            return List.of(new NotificationDraft(userId, NotificationType.MENTOR_APPROVED,
                    "Hồ sơ mentor đã được duyệt",
                    "Chúc mừng! Hồ sơ mentor của bạn đã được phê duyệt. Bạn có thể bắt đầu nhận học viên.",
                    "MENTOR", reference));
        }
        String reason = event.optionalText("reason");
        String message = "Hồ sơ mentor của bạn chưa được phê duyệt"
                + (reason == null ? "" : ". Lý do: " + truncate(reason))
                + ". Vui lòng cập nhật hồ sơ và gửi lại.";
        return List.of(new NotificationDraft(userId, NotificationType.MENTOR_REJECTED,
                "Hồ sơ mentor chưa được duyệt", message, "MENTOR", reference));
    }

    private static String truncate(String reason) {
        return reason.length() <= MAX_REASON_LENGTH ? reason : reason.substring(0, MAX_REASON_LENGTH - 1) + "…";
    }
}
