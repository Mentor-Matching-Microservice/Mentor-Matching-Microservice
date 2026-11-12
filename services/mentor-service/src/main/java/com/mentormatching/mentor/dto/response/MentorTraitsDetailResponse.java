package com.mentormatching.mentor.dto.response;

import java.util.List;

import lombok.Builder;

/** Tính cách và điểm nổi bật công khai của mentor. */
@Builder
public record MentorTraitsDetailResponse(
        List<MentorOptionResponse> personalities,
        List<MentorOptionResponse> highlights) {
}
