package com.mentormatching.booking.dto.response;

import com.mentormatching.booking.entity.Booking;
import com.mentormatching.booking.entity.BookingStatus;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record BookingPaymentContextResponse(
        Long bookingId,
        String studentUserId,
        Long mentorId,
        String mentorUserId,
        BigDecimal totalAmount,
        BookingStatus status
) {
    public static BookingPaymentContextResponse from(Booking booking) {
        return BookingPaymentContextResponse.builder()
                .bookingId(booking.getId())
                .studentUserId(booking.getStudentUserId())
                .mentorId(booking.getMentorId())
                .mentorUserId(booking.getMentorUserId())
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus())
                .build();
    }
}
