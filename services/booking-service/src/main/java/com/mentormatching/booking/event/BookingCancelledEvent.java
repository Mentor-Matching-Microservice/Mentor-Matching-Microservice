package com.mentormatching.booking.event;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record BookingCancelledEvent(
        Long bookingId,
        String cancelledBy,
        String cancelReason,
        LocalDateTime timestamp
) {
}
