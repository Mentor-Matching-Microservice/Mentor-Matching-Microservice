package com.mentormatching.booking.service;

import com.mentormatching.booking.entity.Booking;
import com.mentormatching.booking.entity.BookingStatus;
import com.mentormatching.booking.entity.InboxEvent;
import com.mentormatching.booking.event.BookingConfirmationFailedEvent;
import com.mentormatching.booking.event.BookingConfirmedEvent;
import com.mentormatching.booking.event.BookingEventPublisher;
import com.mentormatching.booking.event.PaymentEventMessage;
import com.mentormatching.booking.repository.BookingRepository;
import com.mentormatching.booking.repository.InboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentSagaService {

    private final BookingRepository bookingRepository;
    private final InboxEventRepository inboxEventRepository;
    private final BookingOutboxService bookingOutboxService;
    private final BookingEventPublisher bookingEventPublisher;
    private final Clock clock;

    @Transactional
    public void handlePaymentEvent(PaymentEventMessage event) {
        if (event == null || event.eventId() == null || event.eventId().isBlank() || event.eventType() == null) {
            log.warn("Discarding malformed or null payment event: {}", event);
            return;
        }

        // Idempotency check via inbox_events table
        if (inboxEventRepository.existsById(event.eventId())) {
            log.info("Duplicate payment event ignored (already processed): eventId={}, type={}",
                    event.eventId(), event.eventType());
            return;
        }

        log.info("Processing payment event: eventId={}, eventType={}, bookingId={}",
                event.eventId(), event.eventType(), event.bookingId());

        if ("PaymentSucceeded".equals(event.eventType())) {
            handlePaymentSucceeded(event);
        } else if ("PaymentRefunded".equals(event.eventType())) {
            handlePaymentRefunded(event);
        } else {
            log.debug("Ignoring unhandled payment eventType: {}", event.eventType());
        }

        // Save receipt in inbox_events to ensure at-most-once/idempotent processing
        InboxEvent inboxEvent = InboxEvent.builder()
                .eventId(event.eventId())
                .eventType(event.eventType())
                .receivedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .processedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .status("PROCESSED")
                .build();
        inboxEventRepository.save(inboxEvent);
    }

    private void handlePaymentSucceeded(PaymentEventMessage event) {
        Long bookingId = parseBookingId(event.bookingId());
        if (bookingId == null) {
            log.error("Cannot process PaymentSucceeded: Invalid booking ID '{}'", event.bookingId());
            bookingOutboxService.recordBookingLifecycleEvent(
                    "BookingConfirmationFailed", 0L, "Invalid booking id format: " + event.bookingId());
            return;
        }

        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            log.error("Booking with id {} not found for PaymentSucceeded", bookingId);
            bookingOutboxService.recordBookingLifecycleEvent(
                    "BookingConfirmationFailed", bookingId, "Booking not found: " + bookingId);
            bookingEventPublisher.publishBookingConfirmationFailed(new BookingConfirmationFailedEvent(
                    UUID.randomUUID().toString(),
                    "BookingConfirmationFailed",
                    Instant.now(),
                    bookingId,
                    "Booking not found: " + bookingId
            ));
            return;
        }

        Booking booking = bookingOpt.get();
        LocalDateTime now = LocalDateTime.now(clock);

        // Scenario 1: Booking is already CANCELLED or REJECTED
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REJECTED) {
            String failureReason = "Payment received but booking was already " + booking.getStatus();
            log.warn("PaymentSucceeded arrived for {} booking id={}. Emitting compensation event BookingConfirmationFailed.",
                    booking.getStatus(), bookingId);

            bookingOutboxService.recordBookingLifecycleEvent("BookingConfirmationFailed", bookingId, failureReason);
            bookingEventPublisher.publishBookingConfirmationFailed(new BookingConfirmationFailedEvent(
                    UUID.randomUUID().toString(),
                    "BookingConfirmationFailed",
                    Instant.now(),
                    bookingId,
                    failureReason
            ));
            return;
        }

        // Scenario 2: Booking is PENDING but payment hold expired
        if (booking.getStatus() == BookingStatus.PENDING
                && booking.getPaymentHoldExpiresAt() != null
                && !booking.getPaymentHoldExpiresAt().isAfter(now)) {
            String failureReason = "Payment received after hold expired at " + booking.getPaymentHoldExpiresAt();
            log.warn("PaymentSucceeded arrived late for expired booking id={}. Cancelling booking and triggering compensation.", bookingId);

            booking.setStatus(BookingStatus.CANCELLED);
            booking.setCancelledBy("SYSTEM_PAYMENT_HOLD_EXPIRED");
            booking.setCancelReason(failureReason);
            bookingRepository.save(booking);

            bookingOutboxService.recordBookingLifecycleEvent("BookingConfirmationFailed", bookingId, failureReason);
            bookingEventPublisher.publishBookingConfirmationFailed(new BookingConfirmationFailedEvent(
                    UUID.randomUUID().toString(),
                    "BookingConfirmationFailed",
                    Instant.now(),
                    bookingId,
                    failureReason
            ));
            return;
        }

        // Scenario 3: Booking is PENDING and valid within hold window -> CONFIRM!
        if (booking.getStatus() == BookingStatus.PENDING) {
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            log.info("Successfully confirmed booking id={} following PaymentSucceeded", bookingId);

            bookingOutboxService.recordBookingLifecycleEvent("BookingConfirmed", bookingId, "Payment confirmed successfully");
            bookingEventPublisher.publishBookingConfirmed(new BookingConfirmedEvent(
                    UUID.randomUUID().toString(),
                    "BookingConfirmed",
                    Instant.now(),
                    booking.getId(),
                    booking.getStudentUserId(),
                    booking.getMentorId(),
                    booking.getMentorUserId(),
                    booking.getTotalAmount(),
                    "VND"
            ));
            return;
        }

        // Scenario 4: Already CONFIRMED or COMPLETED -> Idempotent no-op
        log.info("Booking id={} is already in state {}. No state transition needed.", bookingId, booking.getStatus());
    }

    private void handlePaymentRefunded(PaymentEventMessage event) {
        Long bookingId = parseBookingId(event.bookingId());
        log.info("Received PaymentRefunded event for bookingId={}, amount={}", bookingId, event.refundedAmount());
    }

    private Long parseBookingId(String bookingIdStr) {
        if (bookingIdStr == null || bookingIdStr.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(bookingIdStr);
        } catch (NumberFormatException e) {
            try {
                UUID uuid = UUID.fromString(bookingIdStr);
                return uuid.getLeastSignificantBits() & 0x7FFFFFFFFFFFFFFFL;
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
