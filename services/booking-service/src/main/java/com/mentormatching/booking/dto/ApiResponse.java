package com.mentormatching.booking.dto;

import lombok.Builder;

/**
 * Standard API response envelope.
 */
@Builder
public record ApiResponse<T>(
        int status,
        String code,
        boolean success,
        String message,
        T data
) {
    public static <T> ApiResponse<T> ok(T data, String message) {
        return ApiResponse.<T>builder()
                .status(200)
                .code("SUCCESS")
                .success(true)
                .message(message)
                .data(data)
                .build();
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

