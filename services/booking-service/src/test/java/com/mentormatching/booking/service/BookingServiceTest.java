package com.mentormatching.booking.service;

import com.mentormatching.booking.client.MentorLookupClient;
import com.mentormatching.booking.client.UserLookupClient;
import com.mentormatching.booking.client.dto.MentorBookingContextResponse;
import com.mentormatching.booking.client.dto.MentorMeetingType;
import com.mentormatching.booking.client.dto.UserSummaryResponse;
import com.mentormatching.booking.dto.request.CreateBookingRequest;
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
        // Monday 2026-06-15 (dayOfWeek = 1)
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

        // Mentor has recurring availability on Monday 8:00 - 12:00
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
        assertEquals(new BigDecimal("300000.00"), response.totalAmount()); // 150_000 * 2 hours
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
    void incompatibleMeetingTypeThrowsException() {
        LocalDate bookingDate = LocalDate.of(2026, 6, 15);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .mentorId(10L)
                .mentorSubjectId(100L)
                .bookingDate(bookingDate)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .meetingType(MeetingType.OFFLINE)
                .build();

        when(mentorLookupClient.getBookingContext(10L, 100L)).thenReturn(
                MentorBookingContextResponse.builder()
                        .mentorId(10L)
                        .mentorUserId("mentor-user-10")
                        .meetingType(MentorMeetingType.ONLINE) // Only ONLINE
                        .active(true)
                        .build()
        );

        ApiException ex = assertThrows(ApiException.class, () ->
                bookingService.createBooking("student-1", request));

        assertEquals("Mentor does not support this meeting type", ex.getMessage());
    }

    @Test
    void bookingOutsideAvailabilityThrowsException() {
        LocalDate bookingDate = LocalDate.of(2026, 6, 15);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .mentorId(10L)
                .mentorSubjectId(100L)
                .bookingDate(bookingDate)
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(16, 0))
                .meetingType(MeetingType.ONLINE)
                .build();

        when(mentorLookupClient.getBookingContext(10L, 100L)).thenReturn(
                MentorBookingContextResponse.builder()
                        .mentorId(10L)
                        .mentorUserId("mentor-user-10")
                        .meetingType(MentorMeetingType.HYBRID)
                        .active(true)
                        .build()
        );

        // Availability is only 8:00 - 12:00
        when(mentorAvailabilityRepository.findByMentorId(10L)).thenReturn(List.of(
                MentorAvailability.builder()
                        .mentorId(10L)
                        .availabilityType(AvailabilityType.RECURRING)
                        .dayOfWeek(1)
                        .startTime(LocalTime.of(8, 0))
                        .endTime(LocalTime.of(12, 0))
                        .build()
        ));

        ApiException ex = assertThrows(ApiException.class, () ->
                bookingService.createBooking("student-1", request));

        assertEquals("Mentor is not available at this time", ex.getMessage());
    }

    @Test
    void overlappingBookingThrowsConflict() {
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
                        .mentorUserId("mentor-user-10")
                        .meetingType(MentorMeetingType.HYBRID)
                        .pricePerHour(new BigDecimal("100000.00"))
                        .active(true)
                        .build()
        );

        when(mentorAvailabilityRepository.findByMentorId(10L)).thenReturn(List.of(
                MentorAvailability.builder()
                        .mentorId(10L)
                        .availabilityType(AvailabilityType.RECURRING)
                        .dayOfWeek(1)
                        .startTime(LocalTime.of(8, 0))
                        .endTime(LocalTime.of(12, 0))
                        .build()
        ));

        // Overlapping active booking exists
        when(bookingRepository.existsOverlappingActiveBooking(eq(10L), eq(bookingDate), any(), any()))
                .thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () ->
                bookingService.createBooking("student-1", request));

        assertEquals("The requested time slot is already booked or held by another learner.", ex.getMessage());
        assertEquals("CONFLICT", ex.getCode());
    }
}
