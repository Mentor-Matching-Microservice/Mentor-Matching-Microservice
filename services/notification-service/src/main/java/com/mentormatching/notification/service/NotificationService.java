package com.mentormatching.notification.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.mentormatching.notification.domain.Notification;
import com.mentormatching.notification.dto.NotificationResponse;
import com.mentormatching.notification.dto.PageResponse;
import com.mentormatching.notification.exception.ApiException;
import com.mentormatching.notification.repository.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Hộp thư của người dùng hiện tại. Mọi thao tác đều giới hạn trong thông báo của chính người gọi;
 * thông báo của người khác được xử lý như không tồn tại (404) để không lộ việc nó có tồn tại.
 */
@Service
public class NotificationService {

    public static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository notifications;
    private final MongoTemplate mongoTemplate;
    private final Clock clock;

    public NotificationService(NotificationRepository notifications, MongoTemplate mongoTemplate, Clock clock) {
        this.notifications = notifications;
        this.mongoTemplate = mongoTemplate;
        this.clock = clock;
    }

    /** Trang thông báo, mới nhất trước. {@code page} bắt đầu từ 1. */
    public PageResponse<NotificationResponse> list(UUID userId, int page, int size) {
        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("page must be >= 1 and size between 1 and " + MAX_PAGE_SIZE);
        }
        var pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return PageResponse.from(notifications.findByRecipientUserId(userId.toString(), pageable),
                NotificationResponse::from);
    }

    public long unreadCount(UUID userId) {
        return notifications.countByRecipientUserIdAndReadFalse(userId.toString());
    }

    public NotificationResponse markRead(UUID userId, UUID notificationId) {
        Notification notification = notifications.findByIdAndRecipientUserId(notificationId.toString(), userId.toString())
                .orElseThrow(NotificationService::notFound);
        if (!notification.isRead()) {
            notification.markRead(Instant.now(clock));
            notification = notifications.save(notification);
        }
        return NotificationResponse.from(notification);
    }

    /** Đánh dấu toàn bộ thông báo chưa đọc của người dùng là đã đọc; trả về số thông báo vừa được cập nhật. */
    public long markAllRead(UUID userId) {
        Query unread = Query.query(Criteria.where("recipientUserId").is(userId.toString()).and("read").is(false));
        Update update = new Update().set("read", true).set("readAt", Notification.toStoredPrecision(Instant.now(clock)));
        return mongoTemplate.updateMulti(unread, update, Notification.class).getModifiedCount();
    }

    public void delete(UUID userId, UUID notificationId) {
        if (notifications.deleteByIdAndRecipientUserId(notificationId.toString(), userId.toString()) == 0) {
            throw notFound();
        }
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "Notification not found.");
    }
}
