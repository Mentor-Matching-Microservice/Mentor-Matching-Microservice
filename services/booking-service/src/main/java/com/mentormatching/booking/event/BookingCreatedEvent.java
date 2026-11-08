package com.mentormatching.booking.event;

import com.mentormatching.booking.entity.BookingStatus;
import com.mentormatching.booking.entity.MeetingType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Builder
public record BookingCreatedEvent(
        Long bookingId,
        String studentUserId,
        String studentName,
        Long mentorId,
        String mentorUserId,
        String mentorName,
        Long mentorSubjectId,
        String subjectName,
        String gradeName,
        LocalDate bookingDate,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal pricePerHour,
        BigDecimal totalAmount,
        MeetingType meetingType,
        BookingStatus status,
        LocalDateTime createdAt
) {
}
