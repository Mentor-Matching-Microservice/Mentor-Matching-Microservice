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
import com.mentormatching.booking.event.*;
import com.mentormatching.booking.exception.ApiException;
import com.mentormatching.booking.repository.BookingRepository;
import com.mentormatching.booking.repository.MentorAvailabilityRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final BookingOutboxService bookingOutboxService;
    private final Clock clock;

    @Transactional
    public CreateBookingResponse createBooking(String studentUserId, CreateBookingRequest request) {
        validateTimeRange(request);

        UserSummaryResponse student = userLookupClient.getUserSummary(studentUserId);
        MentorBookingContextResponse mentorContext = mentorLookupClient.getBookingContext(
                request.getMentorId(), request.getMentorSubjectId());

        validateBusinessRules(studentUserId, request, mentorContext);

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

    @Transactional
    public void rejectBookingByMentor(String mentorUserId, Long bookingId, String cancelReason) {
        if (cancelReason == null || cancelReason.isBlank()) {
            throw ApiException.badRequest("Cancel reason is required when rejecting booking");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking not found"));

        ensureMentorOwnership(mentorUserId, booking);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw ApiException.badRequest("Only pending booking can be rejected");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        booking.setStatus(BookingStatus.REJECTED);
        booking.setCancelledBy(mentorUserId);
        booking.setCancelReason(cancelReason.trim());
        booking.setUpdatedAt(now);
        bookingRepository.save(booking);

        bookingEventPublisher.publishBookingRejected(BookingRejectedEvent.builder()
                .bookingId(booking.getId())
                .mentorUserId(mentorUserId)
                .cancelReason(cancelReason.trim())
                .timestamp(now)
                .build());

        bookingOutboxService.recordBookingLifecycleEvent("BookingRejected", booking.getId(), cancelReason.trim());
    }

    @Transactional
    public void completeBookingByMentor(String mentorUserId, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking not found"));

        ensureMentorOwnership(mentorUserId, booking);

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw ApiException.badRequest("Only confirmed booking can be completed");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime sessionEnd = LocalDateTime.of(booking.getBookingDate(), booking.getEndTime());
        if (!now.isAfter(sessionEnd)) {
            throw ApiException.badRequest("Booking can only be completed after the session end time");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setUpdatedAt(now);
        bookingRepository.save(booking);

        bookingEventPublisher.publishBookingCompleted(BookingCompletedEvent.builder()
                .bookingId(booking.getId())
                .mentorUserId(mentorUserId)
                .timestamp(now)
                .build());
    }

    @Transactional
    public void forceCancelBooking(String adminUserId, Long bookingId, String cancelReason) {
        if (cancelReason == null || cancelReason.isBlank()) {
            throw ApiException.badRequest("Cancel reason is required when force cancelling booking");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking not found"));

        if (booking.getStatus() == BookingStatus.COMPLETED
                || booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.REJECTED) {
            throw ApiException.badRequest("Booking in status " + booking.getStatus() + " cannot be force cancelled");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledBy(adminUserId);
        booking.setCancelReason(cancelReason.trim());
        booking.setUpdatedAt(now);
        bookingRepository.save(booking);

        bookingEventPublisher.publishBookingCancelled(BookingCancelledEvent.builder()
                .bookingId(booking.getId())
                .cancelledBy(adminUserId)
                .cancelReason(cancelReason.trim())
                .timestamp(now)
                .build());

        bookingOutboxService.recordBookingLifecycleEvent("BookingCancelled", booking.getId(), cancelReason.trim());
    }

    @Transactional(readOnly = true)
    public CreateBookingResponse getBooking(Long bookingId, String currentUserId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking not found"));

        // If user is neither student nor mentor, and not admin, reject
        if (currentUserId != null && !currentUserId.equals(booking.getStudentUserId())
                && !currentUserId.equals(booking.getMentorUserId())) {
            throw ApiException.forbidden("You do not have permission to view this booking");
        }

        return CreateBookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public List<CreateBookingResponse> getMyBookings(String studentUserId) {
        return bookingRepository.findByStudentUserIdOrderByCreatedAtDescIdDesc(studentUserId).stream()
                .map(CreateBookingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CreateBookingResponse> getMentorBookings(String mentorUserId) {
        return bookingRepository.findByMentorUserIdOrderByCreatedAtDescIdDesc(mentorUserId).stream()
                .map(CreateBookingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<CreateBookingResponse> getBookings(BookingStatus status, MeetingType meetingType,
                                                  String mentorName, String studentName,
                                                  LocalDate bookingDateFrom, LocalDate bookingDateTo,
                                                  Pageable pageable) {
        Specification<Booking> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (meetingType != null) {
                predicates.add(cb.equal(root.get("meetingType"), meetingType));
            }
            if (mentorName != null && !mentorName.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("mentorName")), "%" + mentorName.toLowerCase() + "%"));
            }
            if (studentName != null && !studentName.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("studentName")), "%" + studentName.toLowerCase() + "%"));
            }
            if (bookingDateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bookingDate"), bookingDateFrom));
            }
            if (bookingDateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("bookingDate"), bookingDateTo));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return bookingRepository.findAll(spec, pageable).map(CreateBookingResponse::from);
    }

    @Transactional(readOnly = true)
    public BookingPaymentContextResponse getPaymentContext(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking not found"));
        return BookingPaymentContextResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public BookingReviewEligibilityResponse getReviewEligibility(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking not found"));
        return BookingReviewEligibilityResponse.from(booking);
    }

    @Transactional
    public int expireOverduePendingBookings() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Booking> overdue = bookingRepository.findOverduePendingBookings(now);
        if (overdue.isEmpty()) {
            return 0;
        }

        for (Booking booking : overdue) {
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setCancelReason("Hold expired without payment/confirmation");
            booking.setUpdatedAt(now);
            bookingRepository.save(booking);

            bookingEventPublisher.publishBookingCancelled(BookingCancelledEvent.builder()
                    .bookingId(booking.getId())
                    .cancelledBy("system-scheduler")
                    .cancelReason("Hold expired without payment/confirmation")
                    .timestamp(now)
                    .build());

            bookingOutboxService.recordBookingLifecycleEvent("BookingCancelled", booking.getId(), "Hold expired without payment/confirmation");
        }

        log.info("Expired and cancelled {} overdue pending bookings at {}", overdue.size(), now);
        return overdue.size();
    }

    private void ensureMentorOwnership(String mentorUserId, Booking booking) {
        if (booking.getMentorUserId() != null && !booking.getMentorUserId().equals(mentorUserId)) {
            throw ApiException.notFound("Booking not found");
        }
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
