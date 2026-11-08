package com.mentormatching.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingHoldExpiryScheduler {

    private final BookingService bookingService;

    @Scheduled(fixedDelayString = "${booking.hold-expiry.fixed-delay:60000}")
    public void scanAndExpireOverdueBookings() {
        try {
            int expiredCount = bookingService.expireOverduePendingBookings();
            if (expiredCount > 0) {
                log.info("Hold expiry task completed: {} bookings expired", expiredCount);
            }
        } catch (Exception e) {
            log.error("Failed to run hold expiry task: {}", e.getMessage(), e);
        }
    }
}
