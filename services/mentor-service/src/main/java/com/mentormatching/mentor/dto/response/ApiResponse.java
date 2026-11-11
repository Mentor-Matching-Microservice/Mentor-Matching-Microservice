package com.mentormatching.mentor.dto.response;

import lombok.Builder;

/** Response thành công; lỗi được trả riêng theo ProblemDetail. */
@Builder
public record ApiResponse<T>(
        int status,
        String code,
        boolean success,
        String message,
        T data) {

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .status(200)
                .code("SUCCESS")
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return success(data, message);
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return ApiResponse.<T>builder()
                .status(201)
                .code("CREATED")
                .success(true)
                .message(message)
                .data(data)
                .build();
    }
}
