package com.mentormatching.booking.service;

import com.mentormatching.booking.client.MentorLookupClient;
import com.mentormatching.booking.client.UserLookupClient;
import com.mentormatching.booking.client.dto.MentorBookingContextResponse;
import com.mentormatching.booking.client.dto.MentorMeetingType;
import com.mentormatching.booking.client.dto.UserSummaryResponse;
import com.mentormatching.booking.dto.request.CreateBookingRequest;
import com.mentormatching.booking.dto.response.BookingPaymentContextResponse;
import com.mentormatching.booking.dto.response.BookingReviewEligibilityResponse;
import com.mentormatching.booking.dto.response.CreateBookingResponse;
import com.mentormatching.booking.entity.*;
import com.mentormatching.booking.event.BookingEventPublisher;
import com.mentormatching.booking.exception.ApiException;
import com.mentormatching.booking.repository.BookingRepository;
import com.mentormatching.booking.repository.MentorAvailabilityRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BookingServiceTest {

    private static final ZoneId TEST_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private BookingRepository bookingRepository;
    private MentorAvailabilityRepository mentorAvailabilityRepository;
    private UserLookupClient userLookupClient;
    private MentorLookupClient mentorLookupClient;
    private BookingEventPublisher bookingEventPublisher;
    private BookingService bookingService;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        mentorAvailabilityRepository = mock(MentorAvailabilityRepository.class);
        userLookupClient = mock(UserLookupClient.class);
        mentorLookupClient = mock(MentorLookupClient.class);
        bookingEventPublisher = mock(BookingEventPublisher.class);
        EntityManager entityManager = mock(EntityManager.class, RETURNS_DEEP_STUBS);

        // Fixed at 2026-06-14 08:00:00 (Sunday)
        fixedClock = Clock.fixed(Instant.parse("2026-06-14T01:00:00Z"), TEST_ZONE);

        BookingConcurrencyLock concurrencyLock = new BookingConcurrencyLock(entityManager);

        bookingService = new BookingService(
                bookingRepository,
                mentorAvailabilityRepository,
                userLookupClient,
                mentorLookupClient,
                concurrencyLock,
                bookingEventPublisher,
                fixedClock
        );
    }

    @Test
    void createBookingSucceedsWithSnapshotAndHold() {
        LocalDate bookingDate = LocalDate.of(2026, 6, 15);
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(11, 0); // 2 hours

        CreateBookingRequest request = CreateBookingRequest.builder()
                .mentorId(10L)
                .mentorSubjectId(100L)
                .bookingDate(bookingDate)
                .startTime(startTime)
                .endTime(endTime)
                .meetingType(MeetingType.ONLINE)
                .note("Please prepare algebra review")
                .build();

        when(userLookupClient.getUserSummary("student-uuid-1")).thenReturn(
                UserSummaryResponse.builder()
                        .userId("student-uuid-1")
                        .fullName("Nguyen Van A")
                        .email("a@example.com")
                        .build()
        );

        when(mentorLookupClient.getBookingContext(10L, 100L)).thenReturn(
                MentorBookingContextResponse.builder()
                        .mentorId(10L)
                        .mentorUserId("mentor-user-10")
                        .mentorName("Le Mentor B")
                        .meetingType(MentorMeetingType.HYBRID)
                        .mentorSubjectId(100L)
                        .subjectName("Toán 12")
                        .gradeName("Lớp 12")
                        .pricePerHour(new BigDecimal("150000.00"))
                        .active(true)
                        .build()
        );

        when(mentorAvailabilityRepository.findByMentorId(10L)).thenReturn(List.of(
                MentorAvailability.builder()
                        .id(1L)
                        .mentorId(10L)
                        .availabilityType(AvailabilityType.RECURRING)
                        .dayOfWeek(1)
                        .startTime(LocalTime.of(8, 0))
                        .endTime(LocalTime.of(12, 0))
                        .build()
        ));

        when(bookingRepository.existsOverlappingActiveBooking(10L, bookingDate, startTime, endTime))
                .thenReturn(false);

        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(999L);
            return b;
        });

        CreateBookingResponse response = bookingService.createBooking("student-uuid-1", request);

        assertNotNull(response);
        assertEquals(999L, response.bookingId());
        assertEquals("student-uuid-1", response.studentUserId());
        assertEquals("Nguyen Van A", response.studentName());
        assertEquals(10L, response.mentorId());
        assertEquals("Le Mentor B", response.mentorName());
        assertEquals("Toán 12", response.subjectName());
        assertEquals("Lớp 12", response.gradeName());
        assertEquals(new BigDecimal("150000.00"), response.pricePerHour());
        assertEquals(new BigDecimal("300000.00"), response.totalAmount());
        assertEquals(BookingStatus.PENDING, response.status());
        assertEquals(MeetingType.ONLINE, response.meetingType());
        assertNotNull(response.holdExpiresAt());
        assertTrue(response.holdExpiresAt().isAfter(LocalDateTime.now(fixedClock)));

        verify(bookingEventPublisher).publishBookingCreated(any());
    }

    @Test
    void mentorCannotBookTheirOwnSession() {
        LocalDate bookingDate = LocalDate.of(2026, 6, 15);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .mentorId(10L)
                .mentorSubjectId(100L)
                .bookingDate(bookingDate)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .meetingType(MeetingType.ONLINE)
                .build();

        when(mentorLookupClient.getBookingContext(10L, 100L)).thenReturn(
                MentorBookingContextResponse.builder()
                        .mentorId(10L)
                        .mentorUserId("same-user-id")
                        .mentorName("Le Mentor B")
                        .meetingType(MentorMeetingType.ONLINE)
                        .mentorSubjectId(100L)
                        .pricePerHour(new BigDecimal("100000.00"))
                        .active(true)
                        .build()
        );

        ApiException ex = assertThrows(ApiException.class, () ->
                bookingService.createBooking("same-user-id", request));

        assertEquals("Mentor cannot book their own session", ex.getMessage());
    }

    @Test
    void rejectBookingByMentorSucceeds() {
        Booking booking = Booking.builder()
                .id(50L)
                .mentorUserId("mentor-uuid-10")
                .status(BookingStatus.PENDING)
                .build();

        when(bookingRepository.findById(50L)).thenReturn(Optional.of(booking));

        bookingService.rejectBookingByMentor("mentor-uuid-10", 50L, "Busy with emergency");

        assertEquals(BookingStatus.REJECTED, booking.getStatus());
        assertEquals("mentor-uuid-10", booking.getCancelledBy());
        assertEquals("Busy with emergency", booking.getCancelReason());
        verify(bookingEventPublisher).publishBookingRejected(any());
        verify(bookingRepository).save(booking);
    }

    @Test
    void rejectBookingFailsIfNotPending() {
        Booking booking = Booking.builder()
                .id(50L)
                .mentorUserId("mentor-uuid-10")
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findById(50L)).thenReturn(Optional.of(booking));

        ApiException ex = assertThrows(ApiException.class, () ->
                bookingService.rejectBookingByMentor("mentor-uuid-10", 50L, "Reason"));
        assertEquals("Only pending booking can be rejected", ex.getMessage());
    }

    @Test
    void completeBookingByMentorSucceedsAfterSessionEndTime() {
        // Session ended on 2026-06-13 18:00 (which is before fixedClock 2026-06-14 08:00)
        Booking booking = Booking.builder()
                .id(60L)
                .mentorUserId("mentor-uuid-10")
                .bookingDate(LocalDate.of(2026, 6, 13))
                .startTime(LocalTime.of(16, 0))
                .endTime(LocalTime.of(18, 0))
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findById(60L)).thenReturn(Optional.of(booking));

        bookingService.completeBookingByMentor("mentor-uuid-10", 60L);

        assertEquals(BookingStatus.COMPLETED, booking.getStatus());
        verify(bookingEventPublisher).publishBookingCompleted(any());
        verify(bookingRepository).save(booking);
    }

    @Test
    void completeBookingFailsBeforeSessionEndTime() {
        // Session ends on 2026-06-14 10:00 (which is after fixedClock 2026-06-14 08:00)
        Booking booking = Booking.builder()
                .id(60L)
                .mentorUserId("mentor-uuid-10")
                .bookingDate(LocalDate.of(2026, 6, 14))
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(10, 0))
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findById(60L)).thenReturn(Optional.of(booking));

        ApiException ex = assertThrows(ApiException.class, () ->
                bookingService.completeBookingByMentor("mentor-uuid-10", 60L));
        assertEquals("Booking can only be completed after the session end time", ex.getMessage());
    }

    @Test
    void forceCancelBookingByAdminSucceeds() {
        Booking booking = Booking.builder()
                .id(70L)
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findById(70L)).thenReturn(Optional.of(booking));

        bookingService.forceCancelBooking("admin-uuid-1", 70L, "Violation of platform policy");

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals("admin-uuid-1", booking.getCancelledBy());
        assertEquals("Violation of platform policy", booking.getCancelReason());
        verify(bookingEventPublisher).publishBookingCancelled(any());
        verify(bookingRepository).save(booking);
    }

    @Test
    void expireOverduePendingBookingsCancelsExpiredHolds() {
        Booking overdue1 = Booking.builder()
                .id(81L)
                .status(BookingStatus.PENDING)
                .holdExpiresAt(LocalDateTime.now(fixedClock).minusMinutes(5))
                .build();

        Booking overdue2 = Booking.builder()
                .id(82L)
                .status(BookingStatus.PENDING)
                .holdExpiresAt(LocalDateTime.now(fixedClock).minusMinutes(1))
                .build();

        when(bookingRepository.findOverduePendingBookings(any())).thenReturn(List.of(overdue1, overdue2));

        int count = bookingService.expireOverduePendingBookings();

        assertEquals(2, count);
        assertEquals(BookingStatus.CANCELLED, overdue1.getStatus());
        assertEquals(BookingStatus.CANCELLED, overdue2.getStatus());
        assertEquals("Hold expired without payment/confirmation", overdue1.getCancelReason());
        verify(bookingRepository, times(2)).save(any(Booking.class));
    }

    @Test
    void paymentContextAndReviewEligibility() {
        Booking booking = Booking.builder()
                .id(90L)
                .studentUserId("student-1")
                .mentorId(10L)
                .mentorUserId("mentor-1")
                .totalAmount(new BigDecimal("250000.00"))
                .status(BookingStatus.COMPLETED)
                .build();

        when(bookingRepository.findById(90L)).thenReturn(Optional.of(booking));

        BookingPaymentContextResponse paymentContext = bookingService.getPaymentContext(90L);
        assertEquals(new BigDecimal("250000.00"), paymentContext.totalAmount());
        assertEquals(BookingStatus.COMPLETED, paymentContext.status());

        BookingReviewEligibilityResponse reviewEligibility = bookingService.getReviewEligibility(90L);
        assertTrue(reviewEligibility.eligibleForReview());
    }
}
