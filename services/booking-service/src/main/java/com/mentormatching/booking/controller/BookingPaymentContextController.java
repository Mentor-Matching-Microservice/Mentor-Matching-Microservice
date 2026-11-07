package com.mentormatching.booking.controller;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import com.mentormatching.booking.domain.BookingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/v1/bookings")
public class BookingPaymentContextController {
    private final BookingRepository bookings;

    public BookingPaymentContextController(BookingRepository bookings) {
        this.bookings = bookings;
    }

    @GetMapping("/{bookingId}/payment-context")
    public PaymentContext paymentContext(@PathVariable UUID bookingId, @AuthenticationPrincipal Jwt jwt) {
        var booking = bookings.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        UUID callerId;
        try {
            callerId = UUID.fromString(jwt.getSubject());
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT subject must be a UUID", ex);
        }
        if (!booking.getLearnerId().equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Booking does not belong to current learner");
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (booking.getStatus() != com.mentormatching.booking.domain.BookingStatus.PENDING
                || booking.getPaymentHoldExpiresAt() == null
                || !booking.getPaymentHoldExpiresAt().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking is not within its payment hold window");
        }
        return new PaymentContext(booking.getId(), booking.getLearnerId(), booking.getMentorId(),
                booking.getStatus().name(), booking.getTotalAmount(), booking.getCurrency(),
                booking.getPaymentHoldExpiresAt());
    }

    public record PaymentContext(UUID bookingId, UUID payerUserId, UUID mentorId, String bookingStatus,
                                 java.math.BigDecimal totalAmount, String currency,
                                 OffsetDateTime holdExpiresAt) {}
}
