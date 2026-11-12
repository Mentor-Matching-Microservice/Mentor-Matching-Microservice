package com.mentormatching.mentor.dto.query;

import com.mentormatching.mentor.entity.MentorApprovalStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;

/**
 * Bộ lọc danh sách mentors cho admin; page trong request bắt đầu từ 1.
 */
@Builder
public record GetAdminMentorsQuery(
        @Min(0) @Max(99999) Integer page,
        @Min(1) @Max(100) Integer size,
        MentorApprovalStatus approvalStatus,
        String search) {

    public GetAdminMentorsQuery {
        page = page == null ? 0 : page > 0 ? page - 1 : page;
        size = size == null ? 20 : size;
        search = search != null && !search.isBlank() ? search.trim() : null;
    }
}
