package com.mentormatching.mentor.dto.response;

import lombok.Builder;

/** Một lựa chọn tính cách hoặc điểm nổi bật của mentor. */
@Builder
public record MentorOptionResponse(
        Long id,
        String name,
        String description) {
}
