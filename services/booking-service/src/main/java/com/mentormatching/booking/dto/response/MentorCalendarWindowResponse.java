package com.mentormatching.booking.dto.response;

import lombok.Builder;

import java.time.LocalTime;

@Builder
public record MentorCalendarWindowResponse(
        LocalTime startTime,
        LocalTime endTime
) {
}
