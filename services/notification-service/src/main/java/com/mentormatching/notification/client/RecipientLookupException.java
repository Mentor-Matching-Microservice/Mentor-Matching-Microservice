package com.mentormatching.notification.client;

/**
 * Không tra được người nhận vì service nguồn tạm thời không phản hồi (timeout, 5xx, mất kết nối).
 * Lỗi tạm thời: message được thử lại theo chính sách retry rồi mới vào dead-letter queue.
 */
public class RecipientLookupException extends RuntimeException {

    public RecipientLookupException(String message, Throwable cause) {
        super(message, cause);
    }
}
