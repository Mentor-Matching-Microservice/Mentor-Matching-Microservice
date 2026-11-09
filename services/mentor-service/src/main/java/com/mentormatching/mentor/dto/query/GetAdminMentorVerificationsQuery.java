package com.mentormatching.mentor.dto.query;

import com.mentormatching.mentor.entity.MentorVerificationStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;

/**
 * Bộ lọc danh sách verification cho admin; page trong request bắt đầu từ 1.
 *
 * @param page số trang client, mặc định 1; 0 cũng dùng trang đầu, tối đa 100000
 * @param size kích thước trang, mặc định 20, tối đa 100
 * @param status trạng thái cần lọc; null lấy tất cả
 */
@Builder
public record GetAdminMentorVerificationsQuery(
        @Min(0) @Max(99999) Integer page,
        @Min(1) @Max(100) Integer size,
        MentorVerificationStatus status) {

    public GetAdminMentorVerificationsQuery {
        page = page == null ? 0 : page > 0 ? page - 1 : page;
        size = size == null ? 20 : size;
    }
}
