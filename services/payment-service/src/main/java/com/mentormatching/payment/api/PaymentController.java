package com.mentormatching.payment.api;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.mentormatching.payment.api.PaymentModels.*;
import com.mentormatching.payment.domain.Payment;
import com.mentormatching.payment.service.PaymentService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService service;
    public PaymentController(PaymentService service){this.service=service;}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(@AuthenticationPrincipal Jwt jwt,@RequestHeader("Idempotency-Key") String key,
                                  @Valid @RequestBody CreateRequest request){
        return PaymentResponse.from(service.create(subject(jwt),request.bookingId(),key,jwt.getTokenValue()));
    }

    @GetMapping("/me")
    public Page<PaymentResponse> mine(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){
        if(page<0||size<1)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid page request");
        return service.mine(subject(jwt),page,size).map(PaymentResponse::from);
    }

    @GetMapping("/{id}")
    public PaymentResponse detail(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
        Payment p=service.detail(id);if(!p.getPayerId().equals(subject(jwt))&&!hasRole(jwt,"ADMIN"))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return PaymentResponse.from(p);
    }

    private static UUID subject(Jwt jwt){try{return UUID.fromString(jwt.getSubject());}catch(Exception e){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"JWT subject must be a UUID");}}
    private static boolean hasRole(Jwt jwt,String role){Object roles=jwt.getClaims().get("roles");return roles instanceof java.util.Collection<?> c&&c.contains(role);}
}
