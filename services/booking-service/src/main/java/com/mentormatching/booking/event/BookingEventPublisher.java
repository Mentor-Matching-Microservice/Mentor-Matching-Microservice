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

    public void publishBookingRejected(BookingRejectedEvent event) {
        log.info("Publishing BookingRejectedEvent: bookingId={}, mentorUserId={}, reason={}",
                event.bookingId(), event.mentorUserId(), event.cancelReason());
        applicationEventPublisher.publishEvent(event);
    }

    public void publishBookingCompleted(BookingCompletedEvent event) {
        log.info("Publishing BookingCompletedEvent: bookingId={}, mentorUserId={}",
                event.bookingId(), event.mentorUserId());
        applicationEventPublisher.publishEvent(event);
    }

    public void publishBookingCancelled(BookingCancelledEvent event) {
        log.info("Publishing BookingCancelledEvent: bookingId={}, cancelledBy={}, reason={}",
                event.bookingId(), event.cancelledBy(), event.cancelReason());
        applicationEventPublisher.publishEvent(event);
    }

    public void publishBookingConfirmed(BookingConfirmedEvent event) {
        log.info("Publishing BookingConfirmedEvent: bookingId={}, student={}, mentor={}, amount={}",
                event.bookingId(), event.studentUserId(), event.mentorId(), event.totalAmount());
        applicationEventPublisher.publishEvent(event);
    }

    public void publishBookingConfirmationFailed(BookingConfirmationFailedEvent event) {
        log.info("Publishing BookingConfirmationFailedEvent: bookingId={}, reason={}",
                event.bookingId(), event.reason());
        applicationEventPublisher.publishEvent(event);
    }
}
