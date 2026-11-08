package com.mentormatching.booking.event;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record BookingCompletedEvent(
        Long bookingId,
        String mentorUserId,
        LocalDateTime timestamp
) {
}
