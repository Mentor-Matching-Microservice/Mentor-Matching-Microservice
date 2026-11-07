package com.mentormatching.payment.service;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.mentormatching.payment.api.PaymentModels.BookingPaymentSummary;
import com.mentormatching.payment.domain.Payment;
import com.mentormatching.payment.domain.PaymentRepository;
import com.mentormatching.payment.domain.PaymentStatus;
import com.stripe.model.checkout.Session;

@Service
public class PaymentService {
    private final PaymentRepository payments;
    private final BookingPaymentSummaryClient bookings;
    private final StripePaymentClient stripe;
    public PaymentService(PaymentRepository payments,BookingPaymentSummaryClient bookings,StripePaymentClient stripe){this.payments=payments;this.bookings=bookings;this.stripe=stripe;}

    @Transactional
    public Payment create(UUID payerId,UUID bookingId,String key,String bearer){
        if(key==null||key.isBlank()||key.length()>128)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Idempotency-Key must be 1-128 characters");
        var previous=payments.findByPayerIdAndIdempotencyKey(payerId,key);
        if(previous.isPresent()){
            Payment p=previous.get();if(!p.getBookingId().equals(bookingId))throw new ResponseStatusException(HttpStatus.CONFLICT,"Idempotency key was already used for another booking");
            if(p.getStatus()==PaymentStatus.PENDING&&(p.getExpiresAt()==null||p.getExpiresAt().isAfter(Instant.now())))return p;
            if(p.getStatus()!=PaymentStatus.FAILED)return p;
        }
        BookingPaymentSummary b=bookings.get(bookingId,bearer);
        if(b==null||!bookingId.equals(b.bookingId())||!payerId.equals(b.learnerId()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Booking does not belong to current learner");
        if(!"PENDING".equals(b.status()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Booking is not payable in its current state");
        if(b.totalAmount()==null||b.totalAmount().signum()<=0||b.currency()==null||!b.currency().matches("(?i)[a-z]{3}"))throw new ResponseStatusException(HttpStatus.CONFLICT,"Booking payment snapshot is invalid");
        Payment payment=previous.orElseGet(()->payments.findByBookingId(bookingId).orElse(null));
        if(payment!=null&&!payment.getPayerId().equals(payerId))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Payment belongs to another learner");
        if(payment!=null&&payment.getStatus()!=PaymentStatus.FAILED)return payment;
        if(payment==null)payment=payments.saveAndFlush(new Payment(bookingId,payerId,b.mentorId(),b.totalAmount(),b.currency(),key));
        Session session=stripe.checkout(payment);
        payment.checkout(session.getId(),session.getUrl(),session.getExpiresAt()==null?null:Instant.ofEpochSecond(session.getExpiresAt()));
        return payments.save(payment);
    }

    @Transactional(readOnly=true)
    public Page<Payment> mine(UUID owner,int page,int size){return payments.findByPayerIdOrderByCreatedAtDesc(owner,PageRequest.of(page,Math.min(size,100)));}
    @Transactional(readOnly=true)
    public Payment detail(UUID id){return payments.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Payment not found"));}
}
