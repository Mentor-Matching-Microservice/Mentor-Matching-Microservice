package com.mentormatching.payment.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("payment")
public record PaymentProperties(String bookingServiceUrl, Jwt jwt, Stripe stripe) {
    public record Jwt(String issuer,String audience,String publicKeyLocation) {}
    public record Stripe(String secretKey,String webhookSecret,String successUrl,String cancelUrl,Duration reconcileAfter) {}
}
