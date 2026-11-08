package com.mentormatching.booking.controller;

import com.mentormatching.booking.dto.ApiResponse;
import com.mentormatching.booking.dto.request.CreateBookingRequest;
import com.mentormatching.booking.dto.response.CreateBookingResponse;
import com.mentormatching.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
        String studentUserId = jwt != null ? jwt.getSubject() : "anonymous";
        CreateBookingResponse response = bookingService.createBooking(studentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Booking created successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<CreateBookingResponse>>> getMyBookings(
            @AuthenticationPrincipal Jwt jwt) {
        String studentUserId = jwt != null ? jwt.getSubject() : "anonymous";
        List<CreateBookingResponse> responses = bookingService.getMyBookings(studentUserId);
        return ResponseEntity.ok(ApiResponse.ok(responses, "Get my bookings successfully"));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<CreateBookingResponse>> getBooking(
            @PathVariable Long bookingId) {
        CreateBookingResponse response = bookingService.getBooking(bookingId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Get booking successfully"));
    }
}
