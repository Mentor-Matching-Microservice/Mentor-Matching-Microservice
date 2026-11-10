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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentSagaServiceTest {

    private BookingRepository bookingRepository;
    private InboxEventRepository inboxEventRepository;
    private BookingOutboxService bookingOutboxService;
    private BookingEventPublisher bookingEventPublisher;
    private PaymentSagaService paymentSagaService;
    private Clock fixedClock;

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Instant NOW = Instant.parse("2026-06-15T09:00:00Z");

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        inboxEventRepository = mock(InboxEventRepository.class);
        bookingOutboxService = mock(BookingOutboxService.class);
        bookingEventPublisher = mock(BookingEventPublisher.class);
        fixedClock = Clock.fixed(NOW, ZONE);

        paymentSagaService = new PaymentSagaService(
                bookingRepository,
                inboxEventRepository,
                bookingOutboxService,
                bookingEventPublisher,
                fixedClock
        );
    }

    @Test
    void paymentSucceededConfirmsPendingBookingAndEmitsBookingConfirmed() {
        Booking booking = Booking.builder()
                .id(100L)
                .studentUserId("student-1")
                .mentorId(5L)
                .mentorUserId("mentor-1")
                .status(BookingStatus.PENDING)
                .totalAmount(new BigDecimal("300000.00"))
                .holdExpiresAt(LocalDateTime.now(fixedClock).plusMinutes(15))
                .build();

        when(inboxEventRepository.existsById("evt-success-1")).thenReturn(false);
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        PaymentEventMessage event = new PaymentEventMessage(
                "evt-success-1",
                "PaymentSucceeded",
                NOW,
                "pay-uuid-1",
                "100",
                "student-1",
                "mentor-1",
                new BigDecimal("300000.00"),
                "VND",
                null,
                null
        );

        paymentSagaService.handlePaymentEvent(event);

        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        verify(bookingRepository).save(booking);
        verify(bookingOutboxService).recordBookingLifecycleEvent(eq("BookingConfirmed"), eq(100L), anyString());
        verify(bookingEventPublisher).publishBookingConfirmed(any(BookingConfirmedEvent.class));

        ArgumentCaptor<InboxEvent> inboxCaptor = ArgumentCaptor.forClass(InboxEvent.class);
        verify(inboxEventRepository).save(inboxCaptor.capture());
        assertEquals("evt-success-1", inboxCaptor.getValue().getEventId());
        assertEquals("PROCESSED", inboxCaptor.getValue().getStatus());
    }

    @Test
    void paymentSucceededDuplicateEventIsIgnoredIdempotently() {
        when(inboxEventRepository.existsById("evt-dup-1")).thenReturn(true);

        PaymentEventMessage event = new PaymentEventMessage(
                "evt-dup-1",
                "PaymentSucceeded",
                NOW,
                "pay-uuid-1",
                "100",
                "student-1",
                "mentor-1",
                new BigDecimal("300000.00"),
                "VND",
                null,
                null
        );

        paymentSagaService.handlePaymentEvent(event);

        verify(bookingRepository, never()).findById(anyLong());
        verify(bookingRepository, never()).save(any());
        verify(bookingOutboxService, never()).recordBookingLifecycleEvent(anyString(), anyLong(), anyString());
        verify(bookingEventPublisher, never()).publishBookingConfirmed(any());
    }

    @Test
    void paymentSucceededForCancelledBookingEmitsBookingConfirmationFailedCompensation() {
        Booking booking = Booking.builder()
                .id(101L)
                .studentUserId("student-1")
                .mentorId(5L)
                .status(BookingStatus.CANCELLED)
                .cancelReason("Cancelled by user earlier")
                .build();

        when(inboxEventRepository.existsById("evt-late-cancelled")).thenReturn(false);
        when(bookingRepository.findById(101L)).thenReturn(Optional.of(booking));

        PaymentEventMessage event = new PaymentEventMessage(
                "evt-late-cancelled",
                "PaymentSucceeded",
                NOW,
                "pay-uuid-2",
                "101",
                "student-1",
                "mentor-1",
                new BigDecimal("300000.00"),
                "VND",
                null,
                null
        );

        paymentSagaService.handlePaymentEvent(event);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        verify(bookingRepository, never()).save(any());
        verify(bookingOutboxService).recordBookingLifecycleEvent(eq("BookingConfirmationFailed"), eq(101L), contains("already CANCELLED"));
        verify(bookingEventPublisher).publishBookingConfirmationFailed(any(BookingConfirmationFailedEvent.class));
        verify(inboxEventRepository).save(any(InboxEvent.class));
    }

    @Test
    void paymentSucceededForRejectedBookingEmitsBookingConfirmationFailedCompensation() {
        Booking booking = Booking.builder()
                .id(102L)
                .studentUserId("student-1")
                .mentorId(5L)
                .status(BookingStatus.REJECTED)
                .cancelReason("Mentor unavailable")
                .build();

        when(inboxEventRepository.existsById("evt-rejected")).thenReturn(false);
        when(bookingRepository.findById(102L)).thenReturn(Optional.of(booking));

        PaymentEventMessage event = new PaymentEventMessage(
                "evt-rejected",
                "PaymentSucceeded",
                NOW,
                "pay-uuid-3",
                "102",
                "student-1",
                "mentor-1",
                new BigDecimal("300000.00"),
                "VND",
                null,
                null
        );

        paymentSagaService.handlePaymentEvent(event);

        assertEquals(BookingStatus.REJECTED, booking.getStatus());
        verify(bookingOutboxService).recordBookingLifecycleEvent(eq("BookingConfirmationFailed"), eq(102L), contains("already REJECTED"));
        verify(bookingEventPublisher).publishBookingConfirmationFailed(any(BookingConfirmationFailedEvent.class));
    }

    @Test
    void paymentSucceededAfterHoldExpiredCancelsBookingAndEmitsBookingConfirmationFailed() {
        // Hold expired 5 minutes ago
        LocalDateTime expiredHold = LocalDateTime.now(fixedClock).minusMinutes(5);
        Booking booking = Booking.builder()
                .id(103L)
                .studentUserId("student-1")
                .mentorId(5L)
                .status(BookingStatus.PENDING)
                .holdExpiresAt(expiredHold)
                .build();

        when(inboxEventRepository.existsById("evt-expired")).thenReturn(false);
        when(bookingRepository.findById(103L)).thenReturn(Optional.of(booking));

        PaymentEventMessage event = new PaymentEventMessage(
                "evt-expired",
                "PaymentSucceeded",
                NOW,
                "pay-uuid-4",
                "103",
                "student-1",
                "mentor-1",
                new BigDecimal("300000.00"),
                "VND",
                null,
                null
        );

        paymentSagaService.handlePaymentEvent(event);

        // Booking is cancelled due to late payment
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals("SYSTEM_PAYMENT_HOLD_EXPIRED", booking.getCancelledBy());
        verify(bookingRepository).save(booking);
        verify(bookingOutboxService).recordBookingLifecycleEvent(eq("BookingConfirmationFailed"), eq(103L), contains("expired"));
        verify(bookingEventPublisher).publishBookingConfirmationFailed(any(BookingConfirmationFailedEvent.class));
        verify(inboxEventRepository).save(any(InboxEvent.class));
    }

    @Test
    void paymentRefundedRecordedInInbox() {
        when(inboxEventRepository.existsById("evt-refund-1")).thenReturn(false);

        PaymentEventMessage event = new PaymentEventMessage(
                "evt-refund-1",
                "PaymentRefunded",
                NOW,
                "pay-uuid-5",
                "100",
                "student-1",
                "mentor-1",
                new BigDecimal("300000.00"),
                "VND",
                new BigDecimal("300000.00"),
                "Customer requested refund"
        );

        paymentSagaService.handlePaymentEvent(event);

        verify(inboxEventRepository).save(argThat(i ->
                "evt-refund-1".equals(i.getEventId()) && "PaymentRefunded".equals(i.getEventType())
        ));
    }
}
