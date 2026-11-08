package com.mentormatching.booking.dto.response;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record MentorCalendarResponse(
        Long mentorId,
        LocalDate from,
        LocalDate to,
        List<MentorCalendarDateResponse> dates
) {
}

