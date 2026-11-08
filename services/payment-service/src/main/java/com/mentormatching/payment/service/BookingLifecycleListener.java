package com.mentormatching.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "payment.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class BookingLifecycleListener {
    private final ObjectMapper objectMapper;
    private final PaymentService payments;

    public BookingLifecycleListener(ObjectMapper objectMapper, PaymentService payments) {
        this.objectMapper = objectMapper;
        this.payments = payments;
    }

    @RabbitListener(queues = "${payment.messaging.booking-events-queue:payment.booking-lifecycle}")
    public void receive(String payload) {
        try {
            payments.handleBookingLifecycleEvent(objectMapper.readValue(payload, BookingLifecycleEvent.class));
        } catch (JsonProcessingException ex) {
            throw new AmqpRejectAndDontRequeueException("Booking lifecycle event is not valid JSON", ex);
        }
    }
}
