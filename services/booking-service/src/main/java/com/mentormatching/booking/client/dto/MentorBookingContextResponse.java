package com.mentormatching.booking.client.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record MentorBookingContextResponse(
        Long mentorId,
        String mentorUserId,
        String mentorName,
        MentorMeetingType meetingType,
        Long mentorSubjectId,
        String subjectName,
        String gradeName,
        BigDecimal pricePerHour,
        boolean active
) {
}
