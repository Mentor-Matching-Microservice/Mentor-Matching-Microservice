package com.mentormatching.booking.service;

import com.mentormatching.booking.client.MentorLookupClient;
import com.mentormatching.booking.client.dto.MentorProfileSummary;
import com.mentormatching.booking.dto.request.CreateMentorAvailabilityRequest;
import com.mentormatching.booking.dto.request.UpdateMentorAvailabilityRequest;
import com.mentormatching.booking.dto.response.MentorAvailabilityResponse;
import com.mentormatching.booking.entity.AvailabilityType;
import com.mentormatching.booking.entity.MentorAvailability;
import com.mentormatching.booking.exception.ApiException;
import com.mentormatching.booking.repository.MentorAvailabilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SchedulingServiceTest {

    private MentorAvailabilityRepository mentorAvailabilityRepository;
    private MentorLookupClient mentorLookupClient;
    private SchedulingService schedulingService;

    @BeforeEach
    void setUp() {
        mentorAvailabilityRepository = mock(MentorAvailabilityRepository.class);
        mentorLookupClient = mock(MentorLookupClient.class);
        when(mentorLookupClient.getMentorByUserId("user-uuid-1"))
                .thenReturn(new MentorProfileSummary(5L, "user-uuid-1", "Mentor Name"));
        schedulingService = new SchedulingService(mentorAvailabilityRepository, mentorLookupClient);
    }

    @Test
    void createRecurringAvailabilitySucceeds() {
        CreateMentorAvailabilityRequest request = CreateMentorAvailabilityRequest.builder()
                .availabilityType(AvailabilityType.RECURRING)
                .dayOfWeek(3)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0))
                .build();

        when(mentorAvailabilityRepository.save(any(MentorAvailability.class)))
                .thenAnswer(invocation -> {
                    MentorAvailability a = invocation.getArgument(0);
                    a.setId(100L);
                    return a;
                });

        MentorAvailabilityResponse response = schedulingService.createCurrentMentorAvailability("user-uuid-1", request);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals(5L, response.mentorId());
        assertEquals(AvailabilityType.RECURRING, response.availabilityType());
        assertEquals(3, response.dayOfWeek());
    }

    @Test
    void createRecurringAvailabilityOverlappingThrowsConflict() {
        CreateMentorAvailabilityRequest request = CreateMentorAvailabilityRequest.builder()
                .availabilityType(AvailabilityType.RECURRING)
                .dayOfWeek(3)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0))
                .build();

        when(mentorAvailabilityRepository.existsOverlappingRecurring(eq(5L), eq(3), eq(LocalTime.of(9, 0)), eq(LocalTime.of(12, 0)), isNull()))
                .thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () ->
                schedulingService.createCurrentMentorAvailability("user-uuid-1", request));
        assertEquals("Recurring availability window overlaps with an existing availability window", ex.getMessage());
    }

    @Test
    void createRecurringAvailabilityWithDateThrowsException() {
        CreateMentorAvailabilityRequest request = CreateMentorAvailabilityRequest.builder()
                .availabilityType(AvailabilityType.RECURRING)
                .dayOfWeek(3)
                .availableDate(LocalDate.now())
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0))
                .build();

        ApiException ex = assertThrows(ApiException.class, () ->
                schedulingService.createCurrentMentorAvailability("user-uuid-1", request));
        assertEquals("Available date must be null for recurring availability", ex.getMessage());
    }

    @Test
    void createSpecificDateAvailabilityWithDayOfWeekThrowsException() {
        CreateMentorAvailabilityRequest request = CreateMentorAvailabilityRequest.builder()
                .availabilityType(AvailabilityType.SPECIFIC_DATE)
                .dayOfWeek(3)
                .availableDate(LocalDate.now())
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0))
                .build();

        ApiException ex = assertThrows(ApiException.class, () ->
                schedulingService.createCurrentMentorAvailability("user-uuid-1", request));
        assertEquals("Day of week must be null for specific-date availability", ex.getMessage());
    }

    @Test
    void createAvailabilityWithStartTimeAfterEndTimeThrowsException() {
        CreateMentorAvailabilityRequest request = CreateMentorAvailabilityRequest.builder()
                .availabilityType(AvailabilityType.RECURRING)
                .dayOfWeek(1)
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(10, 0))
                .build();

        ApiException ex = assertThrows(ApiException.class, () ->
                schedulingService.createCurrentMentorAvailability("user-uuid-1", request));
        assertEquals("End time must be after start time", ex.getMessage());
    }

    @Test
    void updateAvailabilityChecksOwnership() {
        MentorAvailability existing = MentorAvailability.builder()
                .id(100L)
                .mentorId(5L)
                .mentorUserId("user-owner")
                .availabilityType(AvailabilityType.RECURRING)
                .dayOfWeek(1)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(10, 0))
                .build();

        when(mentorAvailabilityRepository.findById(100L)).thenReturn(Optional.of(existing));

        UpdateMentorAvailabilityRequest request = UpdateMentorAvailabilityRequest.builder()
                .availabilityType(AvailabilityType.RECURRING)
                .dayOfWeek(2)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .build();

        // Updating with different user
        ApiException ex = assertThrows(ApiException.class, () ->
                schedulingService.updateCurrentMentorAvailability("different-user", 100L, request));
        assertEquals("Mentor availability not found", ex.getMessage());
    }

    @Test
    void deleteAvailabilitySucceedsForOwner() {
        MentorAvailability existing = MentorAvailability.builder()
                .id(100L)
                .mentorId(5L)
                .mentorUserId("user-owner")
                .availabilityType(AvailabilityType.RECURRING)
                .dayOfWeek(1)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(10, 0))
                .build();

        when(mentorAvailabilityRepository.findById(100L)).thenReturn(Optional.of(existing));

        assertDoesNotThrow(() -> schedulingService.deleteCurrentMentorAvailability("user-owner", 100L));
        verify(mentorAvailabilityRepository).delete(existing);
    }
}
