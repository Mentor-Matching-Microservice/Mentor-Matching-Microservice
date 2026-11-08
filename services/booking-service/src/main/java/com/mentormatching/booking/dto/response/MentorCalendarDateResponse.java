package com.mentormatching.booking.dto.response;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record MentorCalendarDateResponse(
        LocalDate date,
        List<MentorCalendarWindowResponse> windows
) {
}
