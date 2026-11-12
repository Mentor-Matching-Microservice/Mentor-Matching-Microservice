package com.mentormatching.booking.controller;

import com.mentormatching.booking.dto.ApiResponse;
import com.mentormatching.booking.dto.response.BookingPaymentContextResponse;
import com.mentormatching.booking.dto.response.BookingReviewEligibilityResponse;
import com.mentormatching.booking.exception.ApiException;
import com.mentormatching.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/bookings")
@RequiredArgsConstructor
public class InternalBookingController {

    private final BookingService bookingService;

    @GetMapping("/{bookingId}/payment-context")
    public ResponseEntity<ApiResponse<BookingPaymentContextResponse>> getPaymentContext(
            @PathVariable String bookingId) {
        Long id = parseBookingId(bookingId);
        BookingPaymentContextResponse response = bookingService.getPaymentContext(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Get booking payment context successfully"));
    }

    @GetMapping("/{bookingId}/review-eligibility")
    public ResponseEntity<ApiResponse<BookingReviewEligibilityResponse>> getReviewEligibility(
            @PathVariable String bookingId) {
        Long id = parseBookingId(bookingId);
        BookingReviewEligibilityResponse response = bookingService.getReviewEligibility(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Get booking review eligibility successfully"));
    }

    private Long parseBookingId(String bookingIdStr) {
        if (bookingIdStr == null || bookingIdStr.isBlank()) {
            throw ApiException.badRequest("Booking ID cannot be blank");
        }
        try {
            return Long.parseLong(bookingIdStr.trim());
        } catch (NumberFormatException e) {
            try {
                UUID uuid = UUID.fromString(bookingIdStr.trim());
                return uuid.getLeastSignificantBits() & 0x7FFFFFFFFFFFFFFFL;
            } catch (Exception ex) {
                throw ApiException.badRequest("Invalid booking ID format: " + bookingIdStr);
            }
        }
    }
}
