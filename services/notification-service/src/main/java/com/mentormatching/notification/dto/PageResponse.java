package com.mentormatching.notification.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Phân trang theo contract của backend cũ và mentor-service: {@code page} bắt đầu từ 1.
 */
public record PageResponse<T>(int page, int pageSize, int totalPages, long totalItems, List<T> data) {

    public static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> mapper) {
        return new PageResponse<>(page.getNumber() + 1, page.getSize(), page.getTotalPages(),
                page.getTotalElements(), page.getContent().stream().map(mapper).toList());
    }
}
