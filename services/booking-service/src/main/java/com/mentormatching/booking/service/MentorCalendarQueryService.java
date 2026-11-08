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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MentorCalendarQueryService {

    private static final long MAX_CALENDAR_RANGE_DAYS = 31;
    private static final List<BookingStatus> SCHEDULE_BLOCKING_STATUSES = List.of(
            BookingStatus.PENDING,
            BookingStatus.CONFIRMED
    );

    private final MentorAvailabilityRepository mentorAvailabilityRepository;
    private final BookingRepository bookingRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public MentorCalendarResponse getMentorCalendar(Long mentorId, LocalDate from, LocalDate to) {
        validateQuery(mentorId, from, to);

        List<MentorAvailability> availabilities = mentorAvailabilityRepository.findCalendarAvailabilities(mentorId, from, to);
        List<Booking> blockingBookings = bookingRepository.findScheduleBlockingBookings(
                mentorId, from, to, SCHEDULE_BLOCKING_STATUSES);

        List<MentorCalendarDateResponse> dates = buildCalendarDates(from, to, availabilities, blockingBookings);

        return MentorCalendarResponse.builder()
                .mentorId(mentorId)
                .from(from)
                .to(to)
                .dates(dates)
                .build();
    }

    private List<MentorCalendarDateResponse> buildCalendarDates(LocalDate from, LocalDate to,
                                                               List<MentorAvailability> availabilities,
                                                               List<Booking> blockingBookings) {
        LocalDateTime bookingCutoff = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);

        Map<LocalDate, List<TimeWindow>> bookingWindowsByDate = blockingBookings.stream()
                .collect(Collectors.groupingBy(Booking::getBookingDate,
                        Collectors.mapping(b -> new TimeWindow(b.getStartTime(), b.getEndTime()),
                                Collectors.toList())));

        return from.datesUntil(to.plusDays(1))
                .map(date -> MentorCalendarDateResponse.builder()
                        .date(date)
                        .windows(buildAvailableWindows(date, availabilities,
                                bookingWindowsByDate.getOrDefault(date, List.of()), bookingCutoff))
                        .build())
                .toList();
    }

    private List<MentorCalendarWindowResponse> buildAvailableWindows(LocalDate date,
                                                                    List<MentorAvailability> availabilities,
                                                                    List<TimeWindow> bookingWindows,
                                                                    LocalDateTime bookingCutoff) {
        if (date.isBefore(bookingCutoff.toLocalDate())) {
            return List.of();
        }

        List<TimeWindow> availableWindows = availabilities.stream()
                .filter(availability -> appliesToDate(availability, date))
                .map(availability -> new TimeWindow(availability.getStartTime(), availability.getEndTime()))
                .toList();

        return subtractWindows(mergeWindows(availableWindows), mergeWindows(bookingWindows)).stream()
                .map(window -> trimPastTime(date, window, bookingCutoff))
                .filter(Objects::nonNull)
                .map(window -> MentorCalendarWindowResponse.builder()
                        .startTime(window.startTime())
                        .endTime(window.endTime())
                        .build())
                .toList();
    }

    private TimeWindow trimPastTime(LocalDate date, TimeWindow window, LocalDateTime bookingCutoff) {
        if (date.isAfter(bookingCutoff.toLocalDate())) {
            return window;
        }

        LocalTime cutoffTime = bookingCutoff.toLocalTime();
        if (!window.endTime().isAfter(cutoffTime)) {
            return null;
        }
        LocalTime startTime = window.startTime().isBefore(cutoffTime) ? cutoffTime : window.startTime();
        return new TimeWindow(startTime, window.endTime());
    }

    private boolean appliesToDate(MentorAvailability availability, LocalDate date) {
        if (availability.getAvailabilityType() == AvailabilityType.RECURRING) {
            return availability.getDayOfWeek() != null
                    && availability.getDayOfWeek() == date.getDayOfWeek().getValue();
        }
        return availability.getAvailableDate() != null && availability.getAvailableDate().isEqual(date);
    }

    private List<TimeWindow> mergeWindows(List<TimeWindow> windows) {
        List<TimeWindow> sortedWindows = windows.stream()
                .sorted(Comparator.comparing(TimeWindow::startTime).thenComparing(TimeWindow::endTime))
                .toList();
        List<TimeWindow> mergedWindows = new ArrayList<>();

        for (TimeWindow window : sortedWindows) {
            if (mergedWindows.isEmpty()) {
                mergedWindows.add(window);
            } else {
                mergeOrAppendWindow(mergedWindows, window);
            }
        }
        return mergedWindows;
    }

    private void mergeOrAppendWindow(List<TimeWindow> mergedWindows, TimeWindow window) {
        TimeWindow previous = mergedWindows.getLast();
        if (window.startTime().isAfter(previous.endTime())) {
            mergedWindows.add(window);
            return;
        }

        LocalTime mergedEnd = window.endTime().isAfter(previous.endTime())
                ? window.endTime()
                : previous.endTime();
        mergedWindows.set(mergedWindows.size() - 1, new TimeWindow(previous.startTime(), mergedEnd));
    }

    private List<TimeWindow> subtractWindows(List<TimeWindow> availableWindows, List<TimeWindow> bookingWindows) {
        List<TimeWindow> remainingWindows = new ArrayList<>(availableWindows);
        for (TimeWindow bookingWindow : bookingWindows) {
            List<TimeWindow> nextWindows = new ArrayList<>();
            for (TimeWindow availableWindow : remainingWindows) {
                subtractWindow(availableWindow, bookingWindow, nextWindows);
            }
            remainingWindows = nextWindows;
        }
        return remainingWindows;
    }

    private void subtractWindow(TimeWindow availableWindow, TimeWindow bookingWindow, List<TimeWindow> result) {
        boolean noOverlap = !bookingWindow.endTime().isAfter(availableWindow.startTime())
                || !bookingWindow.startTime().isBefore(availableWindow.endTime());
        if (noOverlap) {
            result.add(availableWindow);
            return;
        }
        if (bookingWindow.startTime().isAfter(availableWindow.startTime())) {
            result.add(new TimeWindow(availableWindow.startTime(), bookingWindow.startTime()));
        }
        if (bookingWindow.endTime().isBefore(availableWindow.endTime())) {
            result.add(new TimeWindow(bookingWindow.endTime(), availableWindow.endTime()));
        }
    }

    private void validateQuery(Long mentorId, LocalDate from, LocalDate to) {
        if (mentorId == null || mentorId <= 0) {
            throw ApiException.badRequest("Mentor id must be greater than 0");
        }
        if (from == null) {
            throw ApiException.badRequest("Calendar from date is required");
        }
        if (to == null) {
            throw ApiException.badRequest("Calendar to date is required");
        }
        if (from.isAfter(to)) {
            throw ApiException.badRequest("Calendar from date must not be after to date");
        }

        long inclusiveRangeDays = ChronoUnit.DAYS.between(from, to) + 1;
        if (inclusiveRangeDays > MAX_CALENDAR_RANGE_DAYS) {
            throw ApiException.badRequest("Calendar date range must not exceed 31 days");
        }
    }

    public record TimeWindow(LocalTime startTime, LocalTime endTime) {
    }
}
