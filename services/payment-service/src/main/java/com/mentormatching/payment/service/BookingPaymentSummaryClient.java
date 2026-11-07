package com.mentormatching.payment.service;

import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.mentormatching.payment.api.PaymentModels.BookingPaymentSummary;
import com.mentormatching.payment.config.PaymentProperties;

@Component
public class BookingPaymentSummaryClient {
    private final PaymentProperties config; private final RestClient http=RestClient.create();
    public BookingPaymentSummaryClient(PaymentProperties config){this.config=config;}
    public BookingPaymentSummary get(UUID bookingId,String bearerToken){
        try{return http.get().uri(config.bookingServiceUrl()+"/api/v1/bookings/{id}/payment-summary",bookingId)
                .headers(h->h.setBearerAuth(bearerToken)).retrieve().body(BookingPaymentSummary.class);}
        catch(HttpClientErrorException.Forbidden e){throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Booking does not belong to current learner",e);}
        catch(HttpClientErrorException.NotFound e){throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Booking not found",e);}
        catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Booking service could not provide payment details",e);}
    }
}
