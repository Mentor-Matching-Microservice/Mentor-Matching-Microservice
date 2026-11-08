package com.mentormatching.booking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentormatching.booking.entity.OutboxEvent;
import com.mentormatching.booking.event.BookingLifecycleEventMessage;
import com.mentormatching.booking.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingOutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void recordBookingLifecycleEvent(String eventType, Long bookingId, String reason) {
        String eventId = UUID.randomUUID().toString();
        BookingLifecycleEventMessage message = new BookingLifecycleEventMessage(
                eventId,
                eventType,
                Instant.now(),
                String.valueOf(bookingId),
                reason
        );

        try {
            String payload = objectMapper.writeValueAsString(message);
            OutboxEvent outboxEvent = OutboxEvent.of(eventType, String.valueOf(bookingId), payload);
            outboxEventRepository.save(outboxEvent);
            log.info("Recorded OutboxEvent: eventId={}, eventType={}, aggregateId={}",
                    outboxEvent.getId(), eventType, bookingId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize booking lifecycle event: {}", e.getMessage(), e);
            throw new IllegalStateException("Could not serialize booking lifecycle event", e);
        }
    }
}
