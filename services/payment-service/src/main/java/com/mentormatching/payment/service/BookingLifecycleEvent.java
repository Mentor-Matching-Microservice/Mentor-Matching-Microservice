package com.mentormatching.payment.service;

import java.time.Instant;
import java.util.UUID;

public record BookingLifecycleEvent(String eventId, String eventType, Instant occurredAt,
                                    UUID bookingId, String reason) {
}
