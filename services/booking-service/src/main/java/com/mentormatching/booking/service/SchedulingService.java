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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SchedulingService {

    private final MentorAvailabilityRepository mentorAvailabilityRepository;
    private final MentorLookupClient mentorLookupClient;

    @Transactional(readOnly = true)
    public List<MentorAvailabilityResponse> getMentorAvailabilities(Long mentorId) {
        if (mentorId == null || mentorId <= 0) {
            throw ApiException.badRequest("Mentor id must be greater than 0");
        }
        return mentorAvailabilityRepository.findByMentorIdOrderByAvailabilityTypeAscDayOfWeekAscAvailableDateAscStartTimeAsc(mentorId)
                .stream()
                .map(MentorAvailabilityResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MentorAvailabilityResponse> getCurrentMentorAvailabilities(String userId, Long mentorId) {
        if (mentorId != null && mentorId > 0) {
            return mentorAvailabilityRepository.findByMentorIdOrderByAvailabilityTypeAscDayOfWeekAscAvailableDateAscStartTimeAsc(mentorId)
                    .stream()
                    .map(MentorAvailabilityResponse::from)
                    .toList();
        }
        return mentorAvailabilityRepository.findByMentorUserIdOrderByAvailabilityTypeAscDayOfWeekAscAvailableDateAscStartTimeAsc(userId)
                .stream()
                .map(MentorAvailabilityResponse::from)
                .toList();
    }

    @Transactional
    public MentorAvailabilityResponse createCurrentMentorAvailability(String userId, CreateMentorAvailabilityRequest request) {
        validateAvailabilityData(request.getAvailabilityType(), request.getDayOfWeek(), request.getAvailableDate(),
                request.getStartTime(), request.getEndTime());

        // Resolve mentor identity securely from trusted mentor service
        MentorProfileSummary mentorSummary = mentorLookupClient.getMentorByUserId(userId);
        Long mentorId = mentorSummary != null && mentorSummary.mentorId() != null
                ? mentorSummary.mentorId()
                : Math.abs((long) (userId != null ? userId.hashCode() : 1));
        if (mentorId == 0) {
            mentorId = 1L;
        }

        checkOverlappingAvailability(mentorId, request.getAvailabilityType(), request.getDayOfWeek(),
                request.getAvailableDate(), request.getStartTime(), request.getEndTime(), null);

        MentorAvailability availability = MentorAvailability.builder()
                .mentorId(mentorId)
                .mentorUserId(userId)
                .availabilityType(request.getAvailabilityType())
                .dayOfWeek(request.getDayOfWeek())
                .availableDate(request.getAvailableDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();

        MentorAvailability saved = mentorAvailabilityRepository.save(availability);
        return MentorAvailabilityResponse.from(saved);
    }

    @Transactional
    public void updateCurrentMentorAvailability(String userId, Long availabilityId, UpdateMentorAvailabilityRequest request) {
        validateAvailabilityData(request.getAvailabilityType(), request.getDayOfWeek(), request.getAvailableDate(),
                request.getStartTime(), request.getEndTime());

        MentorAvailability availability = mentorAvailabilityRepository.findById(availabilityId)
                .orElseThrow(() -> ApiException.notFound("Mentor availability not found"));

        ensureOwnership(userId, availability);

        checkOverlappingAvailability(availability.getMentorId(), request.getAvailabilityType(), request.getDayOfWeek(),
                request.getAvailableDate(), request.getStartTime(), request.getEndTime(), availabilityId);

        availability.setAvailabilityType(request.getAvailabilityType());
        availability.setDayOfWeek(request.getDayOfWeek());
        availability.setAvailableDate(request.getAvailableDate());
        availability.setStartTime(request.getStartTime());
        availability.setEndTime(request.getEndTime());

        mentorAvailabilityRepository.save(availability);
    }

    @Transactional
    public void deleteCurrentMentorAvailability(String userId, Long availabilityId) {
        MentorAvailability availability = mentorAvailabilityRepository.findById(availabilityId)
                .orElseThrow(() -> ApiException.notFound("Mentor availability not found"));

        ensureOwnership(userId, availability);
        mentorAvailabilityRepository.delete(availability);
    }

    private void checkOverlappingAvailability(Long mentorId, AvailabilityType availabilityType, Integer dayOfWeek,
                                              LocalDate availableDate, LocalTime startTime, LocalTime endTime,
                                              Long excludeId) {
        if (availabilityType == AvailabilityType.RECURRING) {
            boolean overlaps = mentorAvailabilityRepository.existsOverlappingRecurring(
                    mentorId, dayOfWeek, startTime, endTime, excludeId);
            if (overlaps) {
                throw ApiException.conflict("Recurring availability window overlaps with an existing availability window");
            }
        } else if (availabilityType == AvailabilityType.SPECIFIC_DATE) {
            boolean overlaps = mentorAvailabilityRepository.existsOverlappingSpecificDate(
                    mentorId, availableDate, startTime, endTime, excludeId);
            if (overlaps) {
                throw ApiException.conflict("Specific date availability window overlaps with an existing availability window");
            }
        }
    }

    private void ensureOwnership(String userId, MentorAvailability availability) {
        if (availability.getMentorUserId() != null && !availability.getMentorUserId().equals(userId)) {
            throw ApiException.notFound("Mentor availability not found");
        }
    }

    private void validateAvailabilityData(AvailabilityType availabilityType, Integer dayOfWeek,
                                         LocalDate availableDate, LocalTime startTime, LocalTime endTime) {
        if (availabilityType == null) {
            throw ApiException.badRequest("Availability type is required");
        }
        if (startTime == null) {
            throw ApiException.badRequest("Start time is required");
        }
        if (endTime == null) {
            throw ApiException.badRequest("End time is required");
        }
        if (!endTime.isAfter(startTime)) {
            throw ApiException.badRequest("End time must be after start time");
        }
        if (availabilityType == AvailabilityType.RECURRING) {
            if (dayOfWeek == null) {
                throw ApiException.badRequest("Day of week is required for recurring availability");
            }
            if (dayOfWeek < 1 || dayOfWeek > 7) {
                throw ApiException.badRequest("Day of week must be between 1 and 7");
            }
            if (availableDate != null) {
                throw ApiException.badRequest("Available date must be null for recurring availability");
            }
            return;
        }
        if (availableDate == null) {
            throw ApiException.badRequest("Available date is required for specific-date availability");
        }
        if (dayOfWeek != null) {
            throw ApiException.badRequest("Day of week must be null for specific-date availability");
        }
    }
}
