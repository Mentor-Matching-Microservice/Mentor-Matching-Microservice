package com.mentormatching.mentor.dto.response;

import java.util.List;
import lombok.Builder;

/**
 * Kết quả phân trang theo contract của backend cũ.
 *
 * @param page số trang bắt đầu từ 1
 * @param pageSize kích thước trang yêu cầu
 * @param totalPages tổng số trang
 * @param totalItems tổng số bản ghi khớp bộ lọc
 * @param data các bản ghi trên trang hiện tại
 * @param <T> kiểu dữ liệu của mỗi bản ghi
 */
@Builder
public record PageResponse<T>(
        int page,
        int pageSize,
        int totalPages,
        long totalItems,
        List<T> data) {
}
