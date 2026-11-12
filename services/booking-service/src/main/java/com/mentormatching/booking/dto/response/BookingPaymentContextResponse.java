package com.mentormatching.booking.dto.response;

import com.mentormatching.booking.entity.Booking;
import com.mentormatching.booking.entity.BookingStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Builder
public record BookingPaymentContextResponse(
        Long bookingId,
        String studentUserId,
        String payerUserId,
        Long mentorId,
        String mentorUserId,
        BigDecimal totalAmount,
        String currency,
        String bookingStatus,
        BookingStatus status,
        OffsetDateTime holdExpiresAt
) {
    public static BookingPaymentContextResponse from(Booking booking) {
        OffsetDateTime holdOffset = booking.getPaymentHoldExpiresAt() != null
                ? booking.getPaymentHoldExpiresAt().atOffset(ZoneOffset.ofHours(7))
                : null;

        return BookingPaymentContextResponse.builder()
                .bookingId(booking.getId())
                .studentUserId(booking.getStudentUserId())
                .payerUserId(booking.getStudentUserId())
                .mentorId(booking.getMentorId())
                .mentorUserId(booking.getMentorUserId())
                .totalAmount(booking.getTotalAmount())
                .currency("VND")
                .bookingStatus(booking.getStatus() != null ? booking.getStatus().name() : null)
                .status(booking.getStatus())
                .holdExpiresAt(holdOffset)
                .build();
    }
}
