package com.mentormatching.booking.service;

import com.mentormatching.booking.dto.response.MentorCalendarDateResponse;
import com.mentormatching.booking.dto.response.MentorCalendarResponse;
import com.mentormatching.booking.dto.response.MentorCalendarWindowResponse;
import com.mentormatching.booking.entity.AvailabilityType;
import com.mentormatching.booking.entity.Booking;
import com.mentormatching.booking.entity.BookingStatus;
import com.mentormatching.booking.entity.MentorAvailability;
import com.mentormatching.booking.exception.ApiException;
import com.mentormatching.booking.repository.BookingRepository;
import com.mentormatching.booking.repository.MentorAvailabilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MentorCalendarQueryServiceTest {

    private static final ZoneId TEST_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private MentorAvailabilityRepository mentorAvailabilityRepository;
    private BookingRepository bookingRepository;
    private MentorCalendarQueryService mentorCalendarQueryService;

    @BeforeEach
    void setUp() {
        mentorAvailabilityRepository = mock(MentorAvailabilityRepository.class);
        bookingRepository = mock(BookingRepository.class);
        // Fixed at 2026-06-14 00:00:00 (Sunday)
        Clock clock = Clock.fixed(Instant.parse("2026-06-13T17:00:00Z"), TEST_ZONE);
        mentorCalendarQueryService = new MentorCalendarQueryService(
                mentorAvailabilityRepository,
                bookingRepository,
                clock
        );
    }

    @Test
    void getMentorCalendarAcceptsInclusiveRangeOfThirtyOneDays() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 7, 1); // 31 days inclusive

        when(mentorAvailabilityRepository.findCalendarAvailabilities(10L, from, to)).thenReturn(List.of());
        when(bookingRepository.findScheduleBlockingBookings(eq(10L), eq(from), eq(to), any())).thenReturn(List.of());

        MentorCalendarResponse result = mentorCalendarQueryService.getMentorCalendar(10L, from, to);

        assertEquals(10L, result.mentorId());
        assertEquals(from, result.from());
        assertEquals(to, result.to());
        assertEquals(31, result.dates().size());
    }

    @Test
    void getMentorCalendarRejectsRangeOverThirtyOneDays() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 7, 2); // 32 days inclusive

        ApiException ex = assertThrows(ApiException.class, () ->
                mentorCalendarQueryService.getMentorCalendar(10L, from, to));

        assertEquals("Calendar date range must not exceed 31 days", ex.getMessage());
    }

    @Test
    void getMentorCalendarRejectsFromAfterTo() {
        LocalDate from = LocalDate.of(2026, 6, 10);
        LocalDate to = LocalDate.of(2026, 6, 9);

        ApiException ex = assertThrows(ApiException.class, () ->
                mentorCalendarQueryService.getMentorCalendar(10L, from, to));

        assertEquals("Calendar from date must not be after to date", ex.getMessage());
    }

    @Test
    void getMentorCalendarMergesAvailabilitiesAndSubtractsBlockingBookings() {
        // 2026-06-15 is Monday (dayOfWeek = 1)
        LocalDate from = LocalDate.of(2026, 6, 15);
        LocalDate to = LocalDate.of(2026, 6, 17);

        when(mentorAvailabilityRepository.findCalendarAvailabilities(10L, from, to)).thenReturn(List.of(
                // Monday: recurring 9:00 - 12:00
                availability(AvailabilityType.RECURRING, 1, null, LocalTime.of(9, 0), LocalTime.of(12, 0)),
                // Monday: specific date 11:00 - 14:00 (overlaps and extends to 14:00)
                availability(AvailabilityType.SPECIFIC_DATE, null, from, LocalTime.of(11, 0), LocalTime.of(14, 0)),
                // Tuesday: recurring 8:00 - 9:00
                availability(AvailabilityType.RECURRING, 2, null, LocalTime.of(8, 0), LocalTime.of(9, 0))
        ));

        when(bookingRepository.findScheduleBlockingBookings(eq(10L), eq(from), eq(to), any())).thenReturn(List.of(
                // Booking on Monday 10:00 - 11:00
                booking(from, LocalTime.of(10, 0), LocalTime.of(11, 0)),
                // Booking on Monday 12:00 - 13:00
                booking(from, LocalTime.of(12, 0), LocalTime.of(13, 0)),
                // Booking on Tuesday 7:00 - 8:00 (doesn't overlap availability 8:00-9:00)
                booking(from.plusDays(1), LocalTime.of(7, 0), LocalTime.of(8, 0))
        ));

        MentorCalendarResponse result = mentorCalendarQueryService.getMentorCalendar(10L, from, to);

        assertEquals(3, result.dates().size());

        // Monday: [9:00-10:00], [11:00-12:00], [13:00-14:00]
        MentorCalendarDateResponse monday = result.dates().get(0);
        assertEquals(from, monday.date());
        assertEquals(3, monday.windows().size());
        assertEquals(LocalTime.of(9, 0), monday.windows().get(0).startTime());
        assertEquals(LocalTime.of(10, 0), monday.windows().get(0).endTime());
        assertEquals(LocalTime.of(11, 0), monday.windows().get(1).startTime());
        assertEquals(LocalTime.of(12, 0), monday.windows().get(1).endTime());
        assertEquals(LocalTime.of(13, 0), monday.windows().get(2).startTime());
        assertEquals(LocalTime.of(14, 0), monday.windows().get(2).endTime());

        // Tuesday: [8:00-9:00]
        MentorCalendarDateResponse tuesday = result.dates().get(1);
        assertEquals(from.plusDays(1), tuesday.date());
        assertEquals(1, tuesday.windows().size());
        assertEquals(LocalTime.of(8, 0), tuesday.windows().get(0).startTime());
        assertEquals(LocalTime.of(9, 0), tuesday.windows().get(0).endTime());

        // Wednesday: empty
        MentorCalendarDateResponse wednesday = result.dates().get(2);
        assertEquals(to, wednesday.date());
        assertTrue(wednesday.windows().isEmpty());
    }

    private MentorAvailability availability(AvailabilityType type, Integer dayOfWeek, LocalDate date,
                                           LocalTime start, LocalTime end) {
        return MentorAvailability.builder()
                .id(1L)
                .mentorId(10L)
                .availabilityType(type)
                .dayOfWeek(dayOfWeek)
                .availableDate(date)
                .startTime(start)
                .endTime(end)
                .build();
    }

    private Booking booking(LocalDate date, LocalTime start, LocalTime end) {
        return Booking.builder()
                .id(1L)
                .mentorId(10L)
                .bookingDate(date)
                .startTime(start)
                .endTime(end)
                .status(BookingStatus.CONFIRMED)
                .build();
    }
}

