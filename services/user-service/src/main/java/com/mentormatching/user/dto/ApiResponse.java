package com.mentormatching.user.dto;

import lombok.Builder;

/**
 * Response thành công thống nhất với auth-service; lỗi trả ProblemDetail riêng.
 */
@Builder
public record ApiResponse<T>(
        int status,
        String code,
        boolean success,
        String message,
        T data
) {
}
