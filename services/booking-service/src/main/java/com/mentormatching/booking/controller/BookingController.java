package com.mentormatching.booking.controller;

import com.mentormatching.booking.dto.ApiResponse;
import com.mentormatching.booking.dto.request.CreateBookingRequest;
import com.mentormatching.booking.dto.request.ForceCancelBookingRequest;
import com.mentormatching.booking.dto.request.RejectBookingRequest;
import com.mentormatching.booking.dto.response.CreateBookingResponse;
import com.mentormatching.booking.entity.BookingStatus;
import com.mentormatching.booking.entity.MeetingType;
import com.mentormatching.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Validated
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateBookingResponse>> createBooking(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateBookingRequest request) {
        String studentUserId = resolveUserId(jwt);
        CreateBookingResponse response = bookingService.createBooking(studentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Booking created successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<CreateBookingResponse>>> getMyBookings(
            @AuthenticationPrincipal Jwt jwt) {
        String studentUserId = resolveUserId(jwt);
        List<CreateBookingResponse> responses = bookingService.getMyBookings(studentUserId);
        return ResponseEntity.ok(ApiResponse.ok(responses, "Get my bookings successfully"));
    }

    @GetMapping("/mentor/me")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<ApiResponse<List<CreateBookingResponse>>> getMentorBookings(
            @AuthenticationPrincipal Jwt jwt) {
        String mentorUserId = resolveUserId(jwt);
        List<CreateBookingResponse> responses = bookingService.getMentorBookings(mentorUserId);
        return ResponseEntity.ok(ApiResponse.ok(responses, "Get mentor bookings successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Page<CreateBookingResponse>>> getBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) MeetingType meetingType,
            @RequestParam(required = false) String mentorName,
            @RequestParam(required = false) String studentName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bookingDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bookingDateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<CreateBookingResponse> result = bookingService.getBookings(
                status, meetingType, mentorName, studentName, bookingDateFrom, bookingDateTo,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(ApiResponse.ok(result, "Get bookings successfully"));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<CreateBookingResponse>> getBooking(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long bookingId) {
        String currentUserId = resolveUserId(jwt);
        CreateBookingResponse response = bookingService.getBooking(bookingId, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Get booking successfully"));
    }

    @PatchMapping("/{bookingId}/reject")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<ApiResponse<Void>> rejectBooking(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long bookingId,
            @Valid @RequestBody RejectBookingRequest request) {
        String mentorUserId = resolveUserId(jwt);
        bookingService.rejectBookingByMentor(mentorUserId, bookingId, request.getCancelReason());
        return ResponseEntity.ok(ApiResponse.ok(null, "Booking rejected successfully"));
    }

    @PatchMapping("/{bookingId}/complete")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<ApiResponse<Void>> completeBooking(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long bookingId) {
        String mentorUserId = resolveUserId(jwt);
        bookingService.completeBookingByMentor(mentorUserId, bookingId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Booking completed successfully"));
    }

    @PatchMapping("/{bookingId}/force-cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> forceCancelBooking(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long bookingId,
            @Valid @RequestBody ForceCancelBookingRequest request) {
        String adminUserId = resolveUserId(jwt);
        bookingService.forceCancelBooking(adminUserId, bookingId, request.getCancelReason());
        return ResponseEntity.ok(ApiResponse.ok(null, "Booking force cancelled successfully"));
    }

    private String resolveUserId(Jwt jwt) {
        return jwt != null ? jwt.getSubject() : "anonymous";
    }
}
