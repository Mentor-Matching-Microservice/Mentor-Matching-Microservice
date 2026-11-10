package com.mentormatching.booking.event;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record BookingRejectedEvent(
        Long bookingId,
        String mentorUserId,
        String cancelReason,
        LocalDateTime timestamp
) {
}
