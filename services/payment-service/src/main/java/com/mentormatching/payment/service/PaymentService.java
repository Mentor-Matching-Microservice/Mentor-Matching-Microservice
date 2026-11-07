package com.mentormatching.payment.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.mentormatching.payment.api.PaymentModels.BookingPaymentSummary;
import com.mentormatching.payment.domain.*;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

@Service
public class PaymentService {
    private final PaymentRepository payments; private final StripeWebhookEventRepository events;
    private final RefundTransactionRepository refunds;
    private final BookingPaymentSummaryClient bookings; private final StripePaymentClient stripe;
    public PaymentService(PaymentRepository p,StripeWebhookEventRepository e,RefundTransactionRepository refunds,BookingPaymentSummaryClient b,StripePaymentClient s){payments=p;events=e;this.refunds=refunds;bookings=b;stripe=s;}

    @Transactional
    public Payment create(UUID payerId,UUID bookingId,String key,String bearer){
        if(key==null||key.isBlank()||key.length()>128)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Idempotency-Key must be 1-128 characters");
        var previous=payments.findByPayerIdAndIdempotencyKey(payerId,key);
        if(previous.isPresent()){
            Payment p=previous.get();if(!p.getBookingId().equals(bookingId))throw new ResponseStatusException(HttpStatus.CONFLICT,"Idempotency key was already used for another booking");
            if(p.getStatus()==PaymentStatus.PENDING&& (p.getExpiresAt()==null||p.getExpiresAt().isAfter(Instant.now())))return p;
            if(p.getStatus()!=PaymentStatus.FAILED) return p;
        }
        BookingPaymentSummary b=bookings.get(bookingId,bearer);
        if(b==null||!bookingId.equals(b.bookingId())||!payerId.equals(b.learnerId()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Booking does not belong to current learner");
        if(!"PENDING".equals(b.status()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Booking is not payable in its current state");
        if(b.totalAmount()==null||b.totalAmount().signum()<=0||b.currency()==null||!b.currency().matches("(?i)[a-z]{3}"))throw new ResponseStatusException(HttpStatus.CONFLICT,"Booking payment snapshot is invalid");
        Payment payment=previous.orElseGet(()->payments.findByBookingId(bookingId).orElse(null));
        if(payment!=null&&!payment.getPayerId().equals(payerId))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Payment belongs to another learner");
        if(payment!=null&&payment.getStatus()!=PaymentStatus.FAILED) return payment;
        if(payment==null)payment=payments.saveAndFlush(new Payment(bookingId,payerId,b.mentorId(),b.totalAmount(),b.currency(),key));
        Session session=stripe.checkout(payment);
        payment.checkout(session.getId(),session.getUrl(),session.getExpiresAt()==null?null:Instant.ofEpochSecond(session.getExpiresAt()));
        return payments.save(payment);
    }

    @Transactional(readOnly=true)
    public Page<Payment> mine(UUID owner,int page,int size){return payments.findByPayerIdOrderByCreatedAtDesc(owner,PageRequest.of(page,Math.min(size,100)));}
    @Transactional(readOnly=true)
    public Payment detail(UUID id){return payments.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Payment not found"));}

    @Transactional
    public Payment refund(UUID id,BigDecimal requestedAmount,String idempotencyKey,String reason){
        if(idempotencyKey==null||idempotencyKey.isBlank()||idempotencyKey.length()>128)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Idempotency-Key must be 1-128 characters");
        var prior=refunds.findByIdempotencyKey(idempotencyKey);
        if(prior.isPresent()){
            if(!prior.get().getPaymentId().equals(id))throw new ResponseStatusException(HttpStatus.CONFLICT,"Refund key was already used for another payment");
            return payments.findById(id).orElseThrow();
        }
        Payment p=payments.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Payment not found"));
        if(p.getStatus()!=PaymentStatus.PAID&&p.getStatus()!=PaymentStatus.PARTIALLY_REFUNDED)throw new ResponseStatusException(HttpStatus.CONFLICT,"Only paid payments can be refunded");
        if(p.getProviderTransactionId()==null)throw new ResponseStatusException(HttpStatus.CONFLICT,"Payment intent is unavailable for refund");
        BigDecimal remaining=p.getAmount().subtract(p.getRefundedAmount());
        BigDecimal amount=requestedAmount==null?remaining:requestedAmount;
        if(amount.signum()<=0||amount.compareTo(remaining)>0||amount.scale()>2)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Refund amount is invalid");
        var refund=stripe.refund(p,amount,idempotencyKey,reason);
        p.refund(amount); payments.save(p); refunds.save(new RefundTransaction(id,idempotencyKey,refund.getId(),amount)); return p;
    }

    @Transactional
    public void handleStripeWebhook(String payload,String signature){
        String secret=stripe.webhookSecret();if(secret==null||secret.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Stripe webhook is not configured");
        final com.stripe.model.Event event;
        try{event=Webhook.constructEvent(payload,signature,secret);}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid Stripe webhook signature",e);}
        if(events.existsById(event.getId()))return;
        StripeObject object=event.getDataObjectDeserializer().getObject().orElse(null);
        if(object instanceof Session session){
            Payment p=payments.findByProviderReferenceId(session.getId()).orElse(null);
            if(p!=null){
                switch(event.getType()){
                    case "checkout.session.completed","checkout.session.async_payment_succeeded" -> {if("paid".equals(session.getPaymentStatus()))p.paid(session.getPaymentIntent());}
                    case "checkout.session.expired" -> p.failed("Checkout session expired");
                    default -> { }
                }
                payments.save(p);
            }
        } else if(object instanceof PaymentIntent intent){
            if("payment_intent.payment_failed".equals(event.getType())){
                String paymentId=intent.getMetadata()==null?null:intent.getMetadata().get("paymentId");
                var target=paymentId==null?payments.findByProviderTransactionId(intent.getId()):payments.findById(UUID.fromString(paymentId));
                target.ifPresent(p->{p.failed("Stripe payment intent failed");payments.save(p);});
            }
        } else if(object instanceof Charge charge && "charge.refunded".equals(event.getType())){
            String intent=charge.getPaymentIntent();if(intent!=null)payments.findByProviderTransactionId(intent).ifPresent(p->{
                int digits=java.util.Currency.getInstance(p.getCurrency().toUpperCase(java.util.Locale.ROOT)).getDefaultFractionDigits();if(digits<0)digits=2;
                p.reconcileRefund(BigDecimal.valueOf(charge.getAmountRefunded(),digits));payments.save(p);
            });
        }
        events.save(new StripeWebhookEvent(event.getId(),event.getType()));
    }

    @Scheduled(fixedDelayString="${PAYMENT_RECONCILE_DELAY_MS:300000}")
    @Transactional
    public void reconcileExpiredCheckouts(){
        for(Payment p:payments.findTop100ByStatusAndExpiresAtBefore(PaymentStatus.PENDING,Instant.now())){
            if(p.getProviderReferenceId()==null)continue;
            try{Session session=stripe.retrieve(p.getProviderReferenceId());if("paid".equals(session.getPaymentStatus()))p.paid(session.getPaymentIntent());else if("expired".equals(session.getStatus()))p.failed("Checkout session expired");payments.save(p);}catch(RuntimeException ignored){/* next scheduled run retries transient Stripe errors */}
        }
    }
}
