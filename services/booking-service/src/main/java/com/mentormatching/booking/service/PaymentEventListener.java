package com.mentormatching.booking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentormatching.booking.event.PaymentEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "booking.messaging.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final ObjectMapper objectMapper;
    private final PaymentSagaService paymentSagaService;

    @RabbitListener(queues = "${booking.messaging.payment-events-queue:booking.payment-events}")
    public void receivePaymentEvent(String payload) {
        try {
            PaymentEventMessage event = objectMapper.readValue(payload, PaymentEventMessage.class);
            paymentSagaService.handlePaymentEvent(event);
        } catch (JsonProcessingException ex) {
            log.error("Failed to parse payment event JSON: {}", payload, ex);
            throw new AmqpRejectAndDontRequeueException("Payment event is not valid JSON", ex);
        }
    }
}
