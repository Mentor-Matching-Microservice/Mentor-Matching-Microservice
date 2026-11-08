package com.mentormatching.booking.controller;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.mentormatching.booking.domain.BookingRepository;
import com.mentormatching.booking.domain.BookingStatus;
@RestController
@RequestMapping("/api/v1/bookings")
public class BookingPaymentSummaryController {
    private final BookingRepository bookings;
    public BookingPaymentSummaryController(BookingRepository bookings){this.bookings=bookings;}
    @GetMapping("/{id}/payment-summary")
    public PaymentSummary get(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){
        var b=bookings.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Booking not found"));
        UUID caller;try{caller=UUID.fromString(jwt.getSubject());}catch(Exception e){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"JWT subject must be a UUID");}
        if(!b.getLearnerId().equals(caller))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Booking does not belong to learner");
        return new PaymentSummary(b.getId(),b.getLearnerId(),b.getMentorId(),b.getStatus().name(),b.getTotalAmount(),b.getCurrency());
    }
    public record PaymentSummary(UUID bookingId,UUID learnerId,UUID mentorId,String status,BigDecimal totalAmount,String currency){}
}
