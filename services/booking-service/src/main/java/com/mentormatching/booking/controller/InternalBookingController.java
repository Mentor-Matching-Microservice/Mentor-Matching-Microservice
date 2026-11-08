package com.mentormatching.booking.controller;

import com.mentormatching.booking.dto.ApiResponse;
import com.mentormatching.booking.dto.response.BookingPaymentContextResponse;
import com.mentormatching.booking.dto.response.BookingReviewEligibilityResponse;
import com.mentormatching.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/bookings")
@RequiredArgsConstructor
public class InternalBookingController {

    private final BookingService bookingService;

    @GetMapping("/{bookingId}/payment-context")
    public ResponseEntity<ApiResponse<BookingPaymentContextResponse>> getPaymentContext(
            @PathVariable Long bookingId) {
        BookingPaymentContextResponse response = bookingService.getPaymentContext(bookingId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Get booking payment context successfully"));
    }

    @GetMapping("/{bookingId}/review-eligibility")
    public ResponseEntity<ApiResponse<BookingReviewEligibilityResponse>> getReviewEligibility(
            @PathVariable Long bookingId) {
        BookingReviewEligibilityResponse response = bookingService.getReviewEligibility(bookingId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Get booking review eligibility successfully"));
    }
}
