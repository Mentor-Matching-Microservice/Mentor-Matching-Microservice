package com.mentormatching.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("payment")
public record PaymentProperties(String bookingServiceUrl, Jwt jwt, Stripe stripe) {
    public record Jwt(String issuer,String audience,String publicKeyLocation) {}
    public record Stripe(String secretKey,String successUrl,String cancelUrl) {}
}
