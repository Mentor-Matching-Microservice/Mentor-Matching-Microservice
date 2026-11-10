package com.mentormatching.booking.service;

import com.mentormatching.booking.client.MentorLookupClient;
import com.mentormatching.booking.client.UserLookupClient;
import com.mentormatching.booking.client.dto.MentorBookingContextResponse;
import com.mentormatching.booking.client.dto.MentorMeetingType;
import com.mentormatching.booking.client.dto.UserSummaryResponse;
import com.mentormatching.booking.dto.request.CreateBookingRequest;
import com.mentormatching.booking.dto.response.CreateBookingResponse;
import com.mentormatching.booking.entity.AvailabilityType;
import com.mentormatching.booking.entity.Booking;
import com.mentormatching.booking.entity.MeetingType;
import com.mentormatching.booking.entity.MentorAvailability;
import com.mentormatching.booking.event.BookingEventPublisher;
import com.mentormatching.booking.exception.ApiException;
import com.mentormatching.booking.repository.BookingRepository;
import com.mentormatching.booking.repository.MentorAvailabilityRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BookingServiceConcurrencyTest {

    private static final ZoneId TEST_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private BookingRepository bookingRepository;
    private MentorAvailabilityRepository mentorAvailabilityRepository;
    private UserLookupClient userLookupClient;
    private MentorLookupClient mentorLookupClient;
    private BookingEventPublisher bookingEventPublisher;
    private BookingService bookingService;
    private final AtomicBoolean slotOccupied = new AtomicBoolean(false);

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        mentorAvailabilityRepository = mock(MentorAvailabilityRepository.class);
        userLookupClient = mock(UserLookupClient.class);
        mentorLookupClient = mock(MentorLookupClient.class);
        bookingEventPublisher = mock(BookingEventPublisher.class);
        BookingOutboxService bookingOutboxService = mock(BookingOutboxService.class);
        EntityManager entityManager = mock(EntityManager.class, RETURNS_DEEP_STUBS);

        Clock fixedClock = Clock.fixed(Instant.parse("2026-06-14T01:00:00Z"), TEST_ZONE);
        BookingConcurrencyLock concurrencyLock = new BookingConcurrencyLock(entityManager);

        bookingService = new BookingService(
                bookingRepository,
                mentorAvailabilityRepository,
                userLookupClient,
                mentorLookupClient,
                concurrencyLock,
                bookingEventPublisher,
                bookingOutboxService,
                fixedClock
        );

        slotOccupied.set(false);
    }

    @Test
    void concurrentBookingAttemptsOnlyAllowSingleWinner() throws Exception {
        LocalDate bookingDate = LocalDate.of(2026, 6, 15);
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(10, 0);

        when(userLookupClient.getUserSummary(any())).thenReturn(
                UserSummaryResponse.builder().fullName("Learner").build()
        );

        when(mentorLookupClient.getBookingContext(eq(10L), eq(100L))).thenReturn(
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

        // Simulate repository atomic behavior: returns true if already occupied
        when(bookingRepository.existsOverlappingActiveBooking(eq(10L), eq(bookingDate), eq(startTime), eq(endTime)))
                .thenAnswer(inv -> slotOccupied.get());

        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            slotOccupied.set(true);
            Booking b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        List<CreateBookingResponse> successes = Collections.synchronizedList(new ArrayList<>());
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final String studentId = "student-" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    CreateBookingRequest req = CreateBookingRequest.builder()
                            .mentorId(10L)
                            .mentorSubjectId(100L)
                            .bookingDate(bookingDate)
                            .startTime(startTime)
                            .endTime(endTime)
                            .meetingType(MeetingType.ONLINE)
                            .build();
                    CreateBookingResponse res = bookingService.createBooking(studentId, req);
                    successes.add(res);
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Release all threads simultaneously
        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify exactly 1 winner and 4 conflicts
        assertEquals(1, successes.size(), "Exactly 1 booking should succeed");
        assertEquals(4, errors.size(), "All other 4 requests should fail with conflict");

        for (Throwable t : errors) {
            assertEquals(ApiException.class, t.getClass());
            ApiException apiEx = (ApiException) t;
            assertEquals("CONFLICT", apiEx.getCode());
        }
    }
}

