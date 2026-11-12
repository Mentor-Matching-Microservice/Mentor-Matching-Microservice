package com.mentormatching.notification.event;

import java.util.List;
import java.util.Set;

/**
 * Chuyển một loại sự kiện thành các thông báo (người nhận + nội dung tiếng Việt).
 * Mỗi nhóm sự kiện là một bean riêng; thêm sự kiện mới (#81) chỉ cần thêm bean, không sửa luồng xử lý.
 */
public interface EventNotificationMapper {

    /** Các {@code eventType} mà mapper xử lý. */
    Set<String> eventTypes();

    /** @throws InvalidEventException nếu payload thiếu trường bắt buộc */
    List<NotificationDraft> map(DomainEvent event);
}
