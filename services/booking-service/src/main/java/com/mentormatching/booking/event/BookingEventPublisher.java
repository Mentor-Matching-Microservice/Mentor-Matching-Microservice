package com.mentormatching.booking.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publishBookingCreated(BookingCreatedEvent event) {
        log.info("Publishing BookingCreatedEvent: bookingId={}, student={}, mentor={}, date={}, time={}-{}",
                event.bookingId(), event.studentUserId(), event.mentorId(), event.bookingDate(),
                event.startTime(), event.endTime());
        applicationEventPublisher.publishEvent(event);
    }
}
