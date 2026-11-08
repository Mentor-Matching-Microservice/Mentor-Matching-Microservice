package com.mentormatching.booking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mentormatching.booking.event.PaymentEventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentEventListenerTest {

    private ObjectMapper objectMapper;
    private PaymentSagaService paymentSagaService;
    private PaymentEventListener paymentEventListener;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        paymentSagaService = mock(PaymentSagaService.class);
        paymentEventListener = new PaymentEventListener(objectMapper, paymentSagaService);
    }

    @Test
    void receiveValidPaymentEventDispatchesToSagaService() {
        String json = """
                {
                    "eventId": "evt-123",
                    "eventType": "PaymentSucceeded",
                    "occurredAt": "2026-06-15T09:00:00Z",
                    "paymentId": "pay-1",
                    "bookingId": "100",
                    "payerUserId": "user-1",
                    "mentorId": "5",
                    "amount": 250000.00,
                    "currency": "VND"
                }
                """;

        paymentEventListener.receivePaymentEvent(json);

        ArgumentCaptor<PaymentEventMessage> captor = ArgumentCaptor.forClass(PaymentEventMessage.class);
        verify(paymentSagaService).handlePaymentEvent(captor.capture());
        assertEquals("evt-123", captor.getValue().eventId());
        assertEquals("PaymentSucceeded", captor.getValue().eventType());
        assertEquals("100", captor.getValue().bookingId());
    }

    @Test
    void receiveInvalidJsonThrowsAmqpRejectAndDontRequeue() {
        assertThrows(AmqpRejectAndDontRequeueException.class, () ->
                paymentEventListener.receivePaymentEvent("invalid-non-json"));
    }
}
