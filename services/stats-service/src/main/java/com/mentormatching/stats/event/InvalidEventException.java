package com.mentormatching.stats.event;

/**
 * Sự kiện sai định dạng hoặc thiếu trường bắt buộc. Xử lý lại cũng không thành công,
 * nên message được chuyển thẳng vào dead-letter queue để kiểm tra thủ công.
 * Thông điệp chỉ nêu tên trường, không chứa nội dung payload.
 */
public class InvalidEventException extends RuntimeException {

    public InvalidEventException(String message) {
        super(message);
    }
}
