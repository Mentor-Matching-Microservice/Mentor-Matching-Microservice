package com.mentormatching.notification.client;

import java.util.UUID;

/** Học viên và mentor (accountId = {@code sub} JWT) của một booking. */
public record BookingParticipants(String bookingId, UUID learnerUserId, UUID mentorUserId) {
}
