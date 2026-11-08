package com.mentormatching.booking.dto.response;

import com.mentormatching.booking.entity.AvailabilityType;
import com.mentormatching.booking.entity.MentorAvailability;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;

@Builder
public record MentorAvailabilityResponse(
        Long id,
        Long mentorId,
        AvailabilityType availabilityType,
        Integer dayOfWeek,
        LocalDate availableDate,
        LocalTime startTime,
        LocalTime endTime
) {
    public static MentorAvailabilityResponse from(MentorAvailability entity) {
        return MentorAvailabilityResponse.builder()
                .id(entity.getId())
                .mentorId(entity.getMentorId())
                .availabilityType(entity.getAvailabilityType())
                .dayOfWeek(entity.getDayOfWeek())
                .availableDate(entity.getAvailableDate())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .build();
    }
}
