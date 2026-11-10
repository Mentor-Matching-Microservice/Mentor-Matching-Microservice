package com.mentormatching.payment.service;

import java.time.Instant;
import java.util.UUID;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentormatching.payment.domain.OutboxEvent;
import com.mentormatching.payment.domain.OutboxEventRepository;
import com.mentormatching.payment.domain.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentOutboxRecorder {
    private final OutboxEventRepository outbox;
    private final ObjectMapper objectMapper;

    public PaymentOutboxRecorder(OutboxEventRepository outbox, ObjectMapper objectMapper) {
        this.outbox = outbox;
        this.objectMapper = objectMapper;
    }

    public void record(String eventType, Payment payment, String reason) {
        String eventId = UUID.randomUUID().toString();
        PaymentEvent event = new PaymentEvent(eventId, eventType, Instant.now(), payment.getId(),
                payment.getBookingId(), payment.getPayerId(), payment.getMentorId(), payment.getAmount(),
                payment.getCurrency(), payment.getRefundedAmount(), reason);
        try {
            outbox.save(new OutboxEvent(UUID.fromString(eventId), eventType, payment.getId(),
                    objectMapper.writeValueAsString(event)));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize payment event", ex);
        }
    }
}
