package com.mentormatching.payment.api;

import java.util.Collection;
import java.util.UUID;
import com.mentormatching.payment.api.PaymentModels.CreateRequest;
import com.mentormatching.payment.api.PaymentModels.PaymentResponse;
import com.mentormatching.payment.api.PaymentModels.RefundRequest;
import com.mentormatching.payment.domain.Payment;
import com.mentormatching.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(@AuthenticationPrincipal Jwt jwt,
                                  @RequestHeader("Idempotency-Key") String key,
                                  @Valid @RequestBody CreateRequest request) {
        return PaymentResponse.from(service.create(subject(jwt), request.bookingId(), key, jwt.getTokenValue()));
    }

    @GetMapping("/me")
    public Page<PaymentResponse> mine(@AuthenticationPrincipal Jwt jwt,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        validatePage(page, size);
        return service.mine(subject(jwt), page, size).map(PaymentResponse::from);
    }

    @GetMapping("/mentor/me")
    @PreAuthorize("hasRole('MENTOR')")
    public Page<PaymentResponse> mentorPayments(@AuthenticationPrincipal Jwt jwt,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        validatePage(page, size);
        return service.mineAsMentor(subject(jwt), page, size).map(PaymentResponse::from);
    }

    @GetMapping("/{id}")
    public PaymentResponse detail(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        UUID caller = subject(jwt);
        Payment payment = service.detail(id);
        if (!payment.getPayerId().equals(caller)
                && !payment.getMentorId().equals(caller)
                && !hasRole(jwt, "ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return PaymentResponse.from(payment);
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentResponse refund(@PathVariable UUID id,
                                  @RequestHeader("Idempotency-Key") String key,
                                  @Valid @RequestBody(required = false) RefundRequest request) {
        return PaymentResponse.from(service.refund(id, request == null ? null : request.amount(), key,
                request == null ? null : request.reason()));
    }

    @PostMapping("/stripe/webhook")
    @ResponseStatus(HttpStatus.OK)
    public void webhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String signature) {
        service.handleStripeWebhook(payload, signature);
    }

    private static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 100");
        }
    }

    private static UUID subject(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT subject must be a UUID", ex);
        }
    }

    private static boolean hasRole(Jwt jwt, String role) {
        Object roles = jwt.getClaims().get("roles");
        return roles instanceof Collection<?> collection && collection.contains(role);
    }
}
