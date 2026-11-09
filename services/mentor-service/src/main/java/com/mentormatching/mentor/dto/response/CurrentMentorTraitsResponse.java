package com.mentormatching.mentor.dto.response;

import java.util.List;

import lombok.Builder;

/** Các ID option mentor hiện chọn; tên và mô tả lấy từ hai danh sách option. */
@Builder
public record CurrentMentorTraitsResponse(
        List<Long> personalityOptionIds,
        List<Long> highlightOptionIds) {
}
