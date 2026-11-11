package com.mentormatching.mentor.dto.response;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record MentorBookingContextResponse(
        Long mentorId,
        String mentorUserId,
        String mentorName,
        String meetingType,
        Long mentorSubjectId,
        String subjectName,
        String gradeName,
        BigDecimal pricePerHour,
        String currency,
        boolean eligible,
        boolean active
) {
}
