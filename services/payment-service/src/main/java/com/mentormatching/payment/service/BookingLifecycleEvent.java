package com.mentormatching.payment.service;

import java.time.Instant;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record BookingLifecycleEvent(String eventId, String eventType, Instant occurredAt,
                                    UUID bookingId, String reason) {
    @JsonCreator
    public static BookingLifecycleEvent from(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("bookingId") Object bookingId,
            @JsonProperty("reason") String reason) {
        UUID parsedBookingId = null;
        if (bookingId instanceof UUID u) {
            parsedBookingId = u;
        } else if (bookingId instanceof Number num) {
            parsedBookingId = new UUID(0L, num.longValue());
        } else if (bookingId != null) {
            String str = bookingId.toString().trim();
            try {
                parsedBookingId = UUID.fromString(str);
            } catch (IllegalArgumentException e) {
                try {
                    parsedBookingId = new UUID(0L, Long.parseLong(str));
                } catch (NumberFormatException ignored) {}
            }
        }
        return new BookingLifecycleEvent(eventId, eventType, occurredAt, parsedBookingId, reason);
    }
}
