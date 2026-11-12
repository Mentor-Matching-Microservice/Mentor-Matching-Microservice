package com.mentormatching.user.messaging;

/** Sự kiện sai định dạng hoặc thiếu trường bắt buộc; không thử lại, chuyển vào dead-letter queue. */
public class InvalidEventException extends RuntimeException {

    public InvalidEventException(String message) {
        super(message);
    }
}
