package com.mentormatching.catalog.dto.response;

import lombok.Builder;

@Builder
public record ApiResponse<T>(int status, String code, boolean success, String message, T data) {
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder().status(200).code("SUCCESS").success(true)
                .message(message).data(data).build();
    }
}
