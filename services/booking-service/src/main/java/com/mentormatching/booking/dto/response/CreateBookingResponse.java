package com.mentormatching.booking.dto.response;

import com.mentormatching.booking.entity.Booking;
import com.mentormatching.booking.entity.BookingStatus;
import com.mentormatching.booking.entity.MeetingType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Builder
public record CreateBookingResponse(
        Long bookingId,
        String studentUserId,
        String studentName,
        Long mentorId,
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
        LocalDateTime holdExpiresAt,
        String note,
        LocalDateTime createdAt
) {
    public static CreateBookingResponse from(Booking booking) {
        return CreateBookingResponse.builder()
                .bookingId(booking.getId())
                .studentUserId(booking.getStudentUserId())
                .studentName(booking.getStudentName())
                .mentorId(booking.getMentorId())
                .mentorName(booking.getMentorName())
                .mentorSubjectId(booking.getMentorSubjectId())
                .subjectName(booking.getSubjectName())
                .gradeName(booking.getGradeName())
                .bookingDate(booking.getBookingDate())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .pricePerHour(booking.getPricePerHour())
                .totalAmount(booking.getTotalAmount())
                .meetingType(booking.getMeetingType())
                .status(booking.getStatus())
                .holdExpiresAt(booking.getHoldExpiresAt())
                .note(booking.getNote())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}

