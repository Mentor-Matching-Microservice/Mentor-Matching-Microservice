package com.mentormatching.payment.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import com.mentormatching.payment.api.PaymentModels.BookingPaymentContext;
import com.mentormatching.payment.domain.BookingEventReceipt;
import com.mentormatching.payment.domain.BookingEventReceiptRepository;
import com.mentormatching.payment.domain.Payment;
import com.mentormatching.payment.domain.PaymentAttempt;
import com.mentormatching.payment.domain.PaymentAttemptRepository;
import com.mentormatching.payment.domain.PaymentRepository;
import com.mentormatching.payment.domain.PaymentStatus;
import com.mentormatching.payment.domain.RefundTransaction;
import com.mentormatching.payment.domain.RefundTransactionRepository;
import com.mentormatching.payment.domain.WebhookReceipt;
import com.mentormatching.payment.domain.WebhookReceiptRepository;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentService {
    private final PaymentRepository payments;
    private final PaymentAttemptRepository attempts;
    private final WebhookReceiptRepository webhookReceipts;
    private final BookingEventReceiptRepository bookingReceipts;
    private final RefundTransactionRepository refunds;
    private final BookingPaymentSummaryClient bookings;
    private final StripePaymentClient stripe;
    private final PaymentOutboxRecorder outbox;

    public PaymentService(PaymentRepository payments, PaymentAttemptRepository attempts,
                          WebhookReceiptRepository webhookReceipts,
                          BookingEventReceiptRepository bookingReceipts,
                          RefundTransactionRepository refunds, BookingPaymentSummaryClient bookings,
                          StripePaymentClient stripe, PaymentOutboxRecorder outbox) {
        this.payments = payments;
        this.attempts = attempts;
        this.webhookReceipts = webhookReceipts;
        this.bookingReceipts = bookingReceipts;
        this.refunds = refunds;
        this.bookings = bookings;
        this.stripe = stripe;
        this.outbox = outbox;
    }

    @Transactional
    public Payment create(UUID payerId, UUID bookingId, String key, String bearer) {
        validateIdempotencyKey(key);
        Optional<Payment> previous = payments.findByPayerIdAndIdempotencyKey(payerId, key);
        if (previous.isPresent()) {
            Payment existing = previous.get();
            if (!existing.getBookingId().equals(bookingId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Idempotency key was already used for another booking");
            }
            if (existing.getStatus() == PaymentStatus.PENDING && existing.getCheckoutUrl() != null
                    && existing.getExpiresAt() != null && existing.getExpiresAt().isAfter(Instant.now())) {
                return existing;
            }
            if (existing.getStatus() != PaymentStatus.FAILED && existing.getStatus() != PaymentStatus.PENDING) {
                return existing;
            }
        }

        BookingPaymentContext booking = bookings.get(bookingId, bearer);
        if (booking == null || !bookingId.equals(booking.bookingId()) || !payerId.equals(booking.payerUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Booking does not belong to current learner");
        }
        if (!"PENDING".equals(booking.bookingStatus()) || booking.holdExpiresAt() == null
                || !booking.holdExpiresAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Booking is not payable in its current state or its hold expired");
        }
        if (booking.totalAmount() == null || booking.totalAmount().signum() <= 0
                || booking.currency() == null || !booking.currency().matches("(?i)[a-z]{3}")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking payment snapshot is invalid");
        }

        Payment payment = previous.orElseGet(() -> payments.findByBookingId(bookingId).orElse(null));
        if (payment != null && !payment.getPayerId().equals(payerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Payment belongs to another learner");
        }
        if (payment != null && payment.getStatus() != PaymentStatus.FAILED
                && payment.getStatus() != PaymentStatus.PENDING) {
            return payment;
        }
        if (payment == null) {
            payment = payments.saveAndFlush(new Payment(bookingId, payerId, booking.mentorId(),
                    booking.totalAmount(), booking.currency(), key));
        }

        if (stripe.isConfigured()) {
            Session session = stripe.checkout(payment);
            Instant expiry = session.getExpiresAt() == null ? null : Instant.ofEpochSecond(session.getExpiresAt());
            payment.checkout(session.getId(), session.getUrl(), expiry);
            attempts.save(new PaymentAttempt(payment.getId(), payment.getCheckoutAttempt(), session.getId(),
                    session.getUrl(), expiry));
            return payments.save(payment);
        }

        // Development / mock checkout when Stripe key is not configured
        String mockSessionId = "cs_dev_" + payment.getId().toString().replace("-", "");
        String mockCheckoutUrl = stripe.getSuccessUrl(mockSessionId, payment.getId());
        Instant expiry = Instant.now().plus(java.time.Duration.ofMinutes(30));
        payment.checkout(mockSessionId, mockCheckoutUrl, expiry);
        attempts.save(new PaymentAttempt(payment.getId(), payment.getCheckoutAttempt(), mockSessionId,
                mockCheckoutUrl, expiry));
        payment.paid("pi_dev_" + payment.getId().toString().replace("-", ""));
        Payment saved = payments.save(payment);
        outbox.record("PaymentSucceeded", saved, "Development mock payment completed");
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Payment> mine(UUID owner, int page, int size) {
        return payments.findByPayerIdOrderByCreatedAtDesc(owner, PageRequest.of(page, Math.min(size, 100)));
    }

    @Transactional(readOnly = true)
    public Page<Payment> mineAsMentor(UUID mentorId, int page, int size) {
        return payments.findByMentorIdOrderByCreatedAtDesc(mentorId, PageRequest.of(page, Math.min(size, 100)));
    }

    @Transactional(readOnly = true)
    public Payment detail(UUID id) {
        return payments.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
    }

    @Transactional
    public Payment refund(UUID id, BigDecimal requestedAmount, String idempotencyKey, String reason) {
        validateIdempotencyKey(idempotencyKey);
        Payment payment = payments.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        return refundLocked(payment, requestedAmount, idempotencyKey, reason);
    }

    private Payment refundLocked(Payment payment, BigDecimal requestedAmount, String idempotencyKey, String reason) {
        Optional<RefundTransaction> prior = refunds.findByIdempotencyKey(idempotencyKey);
        if (prior.isPresent()) {
            if (!prior.get().getPaymentId().equals(payment.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Refund key was already used for another payment");
            }
            return payment;
        }
        if (payment.getStatus() != PaymentStatus.PAID && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only paid payments can be refunded");
        }
        if (payment.getProviderTransactionId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment intent is unavailable for refund");
        }
        BigDecimal remaining = payment.getAmount().subtract(payment.getRefundedAmount());
        BigDecimal amount = requestedAmount == null ? remaining : requestedAmount;
        if (amount.signum() <= 0 || amount.compareTo(remaining) > 0 || amount.scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refund amount is invalid");
        }
        if (!stripe.isConfigured()) {
            payment.refund(amount);
            payments.save(payment);
            refunds.save(new RefundTransaction(payment.getId(), idempotencyKey, "re_dev_" + payment.getId(), amount));
            outbox.record("PaymentRefunded", payment, reason);
            return payment;
        }
        var providerRefund = stripe.refund(payment, amount, idempotencyKey, reason);
        payment.refund(amount);
        payments.save(payment);
        refunds.save(new RefundTransaction(payment.getId(), idempotencyKey, providerRefund.getId(), amount));
        outbox.record("PaymentRefunded", payment, reason);
        return payment;
    }

    @Transactional
    public void handleStripeWebhook(String payload, String signature) {
        String secret = stripe.webhookSecret();
        if (secret == null || secret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe webhook is not configured");
        }
        final Event event;
        try {
            event = Webhook.constructEvent(payload, signature, secret);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe webhook signature", ex);
        }
        if (webhookReceipts.existsById(event.getId())) {
            return;
        }

        StripeObject object = event.getDataObjectDeserializer().getObject().orElse(null);
        if (object instanceof Session session) {
            handleCheckoutEvent(event, session);
        } else if (object instanceof PaymentIntent intent && "payment_intent.payment_failed".equals(event.getType())) {
            handleFailedIntent(intent);
        } else if (object instanceof Charge charge && "charge.refunded".equals(event.getType())) {
            handleRefundedCharge(charge);
        }
        webhookReceipts.save(new WebhookReceipt(event.getId(), event.getType()));
    }

    private void handleCheckoutEvent(Event event, Session session) {
        Payment payment = payments.findByProviderReferenceId(session.getId()).orElse(null);
        if (payment == null) {
            return;
        }
        PaymentAttempt attempt = attempts.findByProviderReferenceId(session.getId()).orElse(null);
        switch (event.getType()) {
            case "checkout.session.completed", "checkout.session.async_payment_succeeded" -> {
                if ("paid".equals(session.getPaymentStatus()) && payment.paid(session.getPaymentIntent())) {
                    payments.save(payment);
                    if (attempt != null) attempts.save(markCompleted(attempt));
                    outbox.record("PaymentSucceeded", payment, null);
                }
            }
            case "checkout.session.expired" -> {
                if (payment.failed("Checkout session expired")) {
                    payments.save(payment);
                    if (attempt != null) attempts.save(markExpired(attempt));
                    outbox.record("PaymentFailed", payment, "Checkout session expired");
                }
            }
            default -> { }
        }
    }

    private void handleFailedIntent(PaymentIntent intent) {
        String metadataId = intent.getMetadata() == null ? null : intent.getMetadata().get("paymentId");
        Optional<Payment> target = paymentFromIntent(metadataId, intent.getId());
        target.ifPresent(payment -> {
            if (payment.failed("Stripe payment intent failed")) {
                payments.save(payment);
                outbox.record("PaymentFailed", payment, "Stripe payment intent failed");
            }
        });
    }

    private Optional<Payment> paymentFromIntent(String metadataId, String intentId) {
        if (metadataId != null) {
            try {
                Optional<Payment> byId = payments.findById(UUID.fromString(metadataId));
                if (byId.isPresent()) return byId;
            } catch (IllegalArgumentException ignored) {
                // Fall back to the provider transaction ID for malformed metadata.
            }
        }
        return payments.findByProviderTransactionId(intentId);
    }

    private void handleRefundedCharge(Charge charge) {
        String intentId = charge.getPaymentIntent();
        if (intentId == null) return;
        payments.findByProviderTransactionId(intentId).ifPresent(payment -> {
            int digits;
            try {
                digits = Currency.getInstance(payment.getCurrency().toUpperCase(Locale.ROOT)).getDefaultFractionDigits();
            } catch (IllegalArgumentException ex) {
                digits = 2;
            }
            if (digits < 0) digits = 2;
            BigDecimal totalRefunded = BigDecimal.valueOf(charge.getAmountRefunded(), digits);
            if (payment.reconcileRefund(totalRefunded)) {
                payments.save(payment);
                outbox.record("PaymentRefunded", payment, "Reconciled from Stripe charge.refunded");
            }
        });
    }

    @Transactional
    public void handleBookingLifecycleEvent(BookingLifecycleEvent event) {
        if (event == null || event.eventId() == null || event.eventId().isBlank() || event.eventType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking event is incomplete");
        }
        if (bookingReceipts.existsById(event.eventId())) return;
        if (("BookingCancelled".equals(event.eventType())
                || "BookingConfirmationFailed".equals(event.eventType())) && event.bookingId() != null) {
            payments.findByBookingId(event.bookingId()).ifPresent(found -> {
                Payment locked = payments.findByIdForUpdate(found.getId()).orElse(found);
                if ((locked.getStatus() == PaymentStatus.PAID || locked.getStatus() == PaymentStatus.PARTIALLY_REFUNDED)
                        && locked.getAmount().compareTo(locked.getRefundedAmount()) > 0) {
                    refundLocked(locked, null, "booking-event:" + event.eventId(), event.reason());
                }
            });
        }
        bookingReceipts.save(new BookingEventReceipt(event.eventId(), event.eventType()));
    }

    @Scheduled(fixedDelayString = "${PAYMENT_RECONCILE_DELAY_MS:300000}")
    @Transactional
    public void reconcileExpiredCheckouts() {
        for (Payment payment : payments.findTop100ByStatusAndExpiresAtBefore(PaymentStatus.PENDING, Instant.now())) {
            if (payment.getProviderReferenceId() == null || payment.getProviderReferenceId().startsWith("cs_dev_")) continue;
            try {
                Session session = stripe.retrieve(payment.getProviderReferenceId());
                PaymentAttempt attempt = attempts.findByProviderReferenceId(session.getId()).orElse(null);
                if ("paid".equals(session.getPaymentStatus()) && payment.paid(session.getPaymentIntent())) {
                    payments.save(payment);
                    if (attempt != null) attempts.save(markCompleted(attempt));
                    outbox.record("PaymentSucceeded", payment, "Reconciled from Stripe checkout session");
                } else if ("expired".equals(session.getStatus()) && payment.failed("Checkout session expired")) {
                    payments.save(payment);
                    if (attempt != null) attempts.save(markExpired(attempt));
                    outbox.record("PaymentFailed", payment, "Checkout session expired");
                }
            } catch (RuntimeException ignored) {
                // Keep the payment pending; the next scheduled reconciliation retries transient Stripe failures.
            }
        }
    }

    private static PaymentAttempt markCompleted(PaymentAttempt attempt) {
        attempt.completed();
        return attempt;
    }

    private static PaymentAttempt markExpired(PaymentAttempt attempt) {
        attempt.expired();
        return attempt;
    }

    private static void validateIdempotencyKey(String key) {
        if (key == null || key.isBlank() || key.length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Idempotency-Key must be 1-128 characters");
        }
    }
}
