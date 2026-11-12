package com.mentormatching.notification.repository;

import java.util.Optional;

import com.mentormatching.notification.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Mọi truy vấn đều lọc theo {@code recipientUserId}: người dùng không thể đọc/sửa/xoá thông báo của người khác.
 */
public interface NotificationRepository extends MongoRepository<Notification, String> {

    Page<Notification> findByRecipientUserId(String recipientUserId, Pageable pageable);

    long countByRecipientUserIdAndReadFalse(String recipientUserId);

    Optional<Notification> findByIdAndRecipientUserId(String id, String recipientUserId);

    long deleteByIdAndRecipientUserId(String id, String recipientUserId);
}
