package com.mentormatching.notification.domain;

/**
 * Loại thông báo. Giữ nguyên các giá trị của dự án cũ để frontend hiển thị icon/màu như trước,
 * bổ sung {@link #ACCOUNT_REGISTERED} cho sự kiện tạo tài khoản.
 */
public enum NotificationType {
    ACCOUNT_REGISTERED,
    BOOKING_CREATED,
    BOOKING_CONFIRMED,
    BOOKING_CANCELLED,
    BOOKING_REJECTED,
    BOOKING_COMPLETED,
    BOOKING_REMINDER,
    PAYMENT_PENDING,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    PAYMENT_REFUNDED,
    MENTOR_APPROVED,
    MENTOR_REJECTED,
    REVIEW_CREATED,
    SYSTEM
}
