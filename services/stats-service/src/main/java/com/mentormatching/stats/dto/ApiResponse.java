package com.mentormatching.stats.dto;

/**
 * Vỏ response thành công dùng chung với các service khác (user-service, mentor-service):
 * {@code {status, code, success, message, data}}. Lỗi dùng ProblemDetail, không dùng vỏ này.
 */
public record ApiResponse<T>(int status, String code, boolean success, String message, T data) {

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(200, "SUCCESS", true, message, data);
    }
}
