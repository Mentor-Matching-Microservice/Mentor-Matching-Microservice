package com.mentormatching.booking.service;

import com.mentormatching.booking.client.MentorLookupClient;
import com.mentormatching.booking.client.UserLookupClient;
import com.mentormatching.booking.client.dto.MentorBookingContextResponse;
import com.mentormatching.booking.client.dto.MentorMeetingType;
import com.mentormatching.booking.client.dto.UserSummaryResponse;
import com.mentormatching.booking.dto.request.CreateBookingRequest;
import com.mentormatching.booking.dto.response.CreateBookingResponse;
import com.mentormatching.booking.entity.*;
import com.mentormatching.booking.event.BookingCreatedEvent;
import com.mentormatching.booking.event.BookingEventPublisher;
import com.mentormatching.booking.exception.ApiException;
import com.mentormatching.booking.repository.BookingRepository;
import com.mentormatching.booking.repository.MentorAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private static final int DEFAULT_HOLD_MINUTES = 15;

    private final BookingRepository bookingRepository;
    private final MentorAvailabilityRepository mentorAvailabilityRepository;
    private final UserLookupClient userLookupClient;
    private final MentorLookupClient mentorLookupClient;
    private final BookingConcurrencyLock bookingConcurrencyLock;
    private final BookingEventPublisher bookingEventPublisher;
    private final Clock clock;

    @Transactional
    public CreateBookingResponse createBooking(String studentUserId, CreateBookingRequest request) {
        validateTimeRange(request);

        // Fetch authoritative profile summaries
        UserSummaryResponse student = userLookupClient.getUserSummary(studentUserId);
        MentorBookingContextResponse mentorContext = mentorLookupClient.getBookingContext(
                request.getMentorId(), request.getMentorSubjectId());

        // Domain validations
        validateBusinessRules(studentUserId, request, mentorContext);

        // Concurrency-safe slot reservation
        return bookingConcurrencyLock.executeWithLock(request.getMentorId(), request.getBookingDate(), () -> {
            boolean overlapping = bookingRepository.existsOverlappingActiveBooking(
                    request.getMentorId(),
                    request.getBookingDate(),
                    request.getStartTime(),
                    request.getEndTime());

            if (overlapping) {
                throw ApiException.conflict("The requested time slot is already booked or held by another learner.");
            }

            BigDecimal totalAmount = calculateTotalAmount(mentorContext.pricePerHour(), request);
            LocalDateTime now = LocalDateTime.now(clock);
            LocalDateTime holdExpiresAt = now.plusMinutes(DEFAULT_HOLD_MINUTES);

            Booking booking = Booking.builder()
                    .studentUserId(studentUserId)
                    .studentName(student != null ? student.fullName() : null)
                    .mentorId(request.getMentorId())
                    .mentorUserId(mentorContext.mentorUserId())
                    .mentorName(mentorContext.mentorName())
                    .mentorSubjectId(request.getMentorSubjectId())
                    .subjectName(mentorContext.subjectName())
                    .gradeName(mentorContext.gradeName())
                    .bookingDate(request.getBookingDate())
                    .startTime(request.getStartTime())
                    .endTime(request.getEndTime())
                    .pricePerHour(mentorContext.pricePerHour())
                    .totalAmount(totalAmount)
                    .meetingType(request.getMeetingType())
                    .status(BookingStatus.PENDING)
                    .holdExpiresAt(holdExpiresAt)
                    .note(request.getNote())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            Booking saved = bookingRepository.save(booking);

            bookingEventPublisher.publishBookingCreated(BookingCreatedEvent.builder()
                    .bookingId(saved.getId())
                    .studentUserId(saved.getStudentUserId())
                    .studentName(saved.getStudentName())
                    .mentorId(saved.getMentorId())
                    .mentorUserId(saved.getMentorUserId())
                    .mentorName(saved.getMentorName())
                    .mentorSubjectId(saved.getMentorSubjectId())
                    .subjectName(saved.getSubjectName())
                    .gradeName(saved.getGradeName())
                    .bookingDate(saved.getBookingDate())
                    .startTime(saved.getStartTime())
                    .endTime(saved.getEndTime())
                    .pricePerHour(saved.getPricePerHour())
                    .totalAmount(saved.getTotalAmount())
                    .meetingType(saved.getMeetingType())
                    .status(saved.getStatus())
                    .createdAt(saved.getCreatedAt())
                    .build());

            return CreateBookingResponse.from(saved);
        });
    }

    @Transactional(readOnly = true)
    public CreateBookingResponse getBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking not found"));
        return CreateBookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public List<CreateBookingResponse> getMyBookings(String studentUserId) {
        return bookingRepository.findByStudentUserId(studentUserId).stream()
                .map(CreateBookingResponse::from)
                .toList();
    }

    private void validateTimeRange(CreateBookingRequest request) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw ApiException.badRequest("Booking end time must be after start time");
        }

        LocalDateTime bookingStartAt = LocalDateTime.of(request.getBookingDate(), request.getStartTime());
        if (!bookingStartAt.isAfter(LocalDateTime.now(clock))) {
            throw ApiException.badRequest("Booking time must be in the future");
        }
    }

    private void validateBusinessRules(String studentUserId, CreateBookingRequest request,
                                       MentorBookingContextResponse mentorContext) {
        if (mentorContext == null) {
            throw ApiException.notFound("Mentor booking context not found");
        }

        if (!mentorContext.active()) {
            throw ApiException.badRequest("Mentor subject is not active");
        }

        if (studentUserId != null && studentUserId.equals(mentorContext.mentorUserId())) {
            throw ApiException.badRequest("Mentor cannot book their own session");
        }

        validateMeetingType(request.getMeetingType(), mentorContext.meetingType());
        validateMentorAvailability(request);
    }

    private void validateMeetingType(MeetingType bookingMeetingType, MentorMeetingType mentorMeetingType) {
        if (mentorMeetingType == MentorMeetingType.HYBRID) {
            return;
        }
        if (mentorMeetingType == null || !mentorMeetingType.name().equals(bookingMeetingType.name())) {
            throw ApiException.badRequest("Mentor does not support this meeting type");
        }
    }

    private void validateMentorAvailability(CreateBookingRequest request) {
        List<MentorAvailability> availabilities = mentorAvailabilityRepository.findByMentorId(request.getMentorId());
        boolean isAvailable = availabilities.stream().anyMatch(a -> {
            boolean dateMatches;
            if (a.getAvailabilityType() == AvailabilityType.RECURRING) {
                dateMatches = a.getDayOfWeek() != null
                        && a.getDayOfWeek() == request.getBookingDate().getDayOfWeek().getValue();
            } else {
                dateMatches = a.getAvailableDate() != null
                        && a.getAvailableDate().isEqual(request.getBookingDate());
            }
            return dateMatches
                    && !request.getStartTime().isBefore(a.getStartTime())
                    && !request.getEndTime().isAfter(a.getEndTime());
        });

        if (!isAvailable) {
            throw ApiException.badRequest("Mentor is not available at this time");
        }
    }

    private BigDecimal calculateTotalAmount(BigDecimal pricePerHour, CreateBookingRequest request) {
        if (pricePerHour == null || pricePerHour.compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("Price per hour must be positive");
        }
        long durationMinutes = Duration.between(request.getStartTime(), request.getEndTime()).toMinutes();
        return pricePerHour.multiply(BigDecimal.valueOf(durationMinutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }
}
