package com.mentormatching.notification.service;

/** Cho phép test ở package khác gọi kiểm tra nội bộ của {@link NotificationIngestionService}. */
public final class NotificationIngestionServiceTestAccess {

    private NotificationIngestionServiceTestAccess() {
    }

    public static boolean isTransient(Throwable failure) {
        return NotificationIngestionService.isTransientTransactionError(failure);
    }
}
