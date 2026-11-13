package com.mentormatching.payment.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import com.mentormatching.payment.config.PaymentProperties;
import com.mentormatching.payment.domain.Payment;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class StripePaymentClient {
    private final PaymentProperties.Stripe config;
    public StripePaymentClient(PaymentProperties properties){config=properties.stripe();}
    public Session checkout(Payment payment){
        ensureKey();
        long cents=toMinorUnits(payment.getAmount(),payment.getCurrency());
        SessionCreateParams params=SessionCreateParams.builder().setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(config.successUrl()).setCancelUrl(config.cancelUrl())
                .putAllMetadata(Map.of("paymentId",payment.getId().toString(),"bookingId",payment.getBookingId().toString(),"payerId",payment.getPayerId().toString()))
                .setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder()
                        .putMetadata("paymentId",payment.getId().toString()).putMetadata("bookingId",payment.getBookingId().toString()).build())
                .addLineItem(SessionCreateParams.LineItem.builder().setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder().setCurrency(payment.getCurrency())
                                .setUnitAmount(cents).setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder().setName("Mentor session " + payment.getBookingId()).build()).build()).build()).build();
        try{return Session.create(params,options("checkout:"+payment.getId()+":"+(payment.getCheckoutAttempt()+1)));}
        catch(StripeException e){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Stripe checkout creation failed",e);}
    }
    public Refund refund(Payment payment,BigDecimal amount,String idempotencyKey,String reason){
        ensureKey();
        long cents=toMinorUnits(amount,payment.getCurrency());
        RefundCreateParams.Builder builder=RefundCreateParams.builder().setPaymentIntent(payment.getProviderTransactionId()).setAmount(cents);
        if(reason!=null&&!reason.isBlank())builder.putMetadata("reason",reason.substring(0,Math.min(reason.length(),500)));
        try{return Refund.create(builder.build(),options("refund:"+idempotencyKey));}
        catch(StripeException e){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Stripe refund failed",e);}
    }
    public boolean isConfigured() {
        return config != null && config.secretKey() != null && !config.secretKey().isBlank()
                && !config.secretKey().startsWith("mock");
    }

    public String getSuccessUrl(String sessionId, java.util.UUID paymentId) {
        String base = (config != null && config.successUrl() != null && !config.successUrl().isBlank())
                ? config.successUrl()
                : "http://localhost:5173/payments/success?session_id={CHECKOUT_SESSION_ID}";
        String resolved = base.replace("{CHECKOUT_SESSION_ID}", sessionId != null ? sessionId : "");
        return resolved + (resolved.contains("?") ? "&" : "?") + "payment_id=" + paymentId;
    }

    public Session retrieve(String sessionId) {
        if (!isConfigured() || (sessionId != null && sessionId.startsWith("cs_dev_"))) {
            return null;
        }
        ensureKey();
        try {
            return Session.retrieve(sessionId, options(null));
        } catch (StripeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe session retrieval failed", e);
        }
    }

    public String webhookSecret() {
        return config != null ? config.webhookSecret() : null;
    }
    private RequestOptions options(String key){RequestOptions.RequestOptionsBuilder b=RequestOptions.builder().setApiKey(config.secretKey());if(key!=null)b.setIdempotencyKey(key);return b.build();}
    private void ensureKey(){if(config==null||config.secretKey()==null||config.secretKey().isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Stripe is not configured");}
    static long toMinorUnits(BigDecimal amount,String currency){
        try {
            int digits=java.util.Currency.getInstance(currency.toUpperCase(java.util.Locale.ROOT)).getDefaultFractionDigits();
            if(digits<0)digits=2;
            return amount.multiply(BigDecimal.TEN.pow(digits)).setScale(0,RoundingMode.UNNECESSARY).longValueExact();
        } catch(IllegalArgumentException|ArithmeticException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Amount or currency is not valid for Stripe",ex);
        }
    }
}
