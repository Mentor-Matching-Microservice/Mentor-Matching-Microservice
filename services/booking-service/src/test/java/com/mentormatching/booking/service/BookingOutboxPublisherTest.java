package com.mentormatching.booking.service;

import com.mentormatching.booking.entity.OutboxEvent;
import com.mentormatching.booking.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BookingOutboxPublisherTest {

    private OutboxEventRepository outboxEventRepository;
    private RabbitTemplate rabbitTemplate;
    private BookingOutboxPublisher outboxPublisher;

    @BeforeEach
    void setUp() {
        outboxEventRepository = mock(OutboxEventRepository.class);
        rabbitTemplate = mock(RabbitTemplate.class);
        outboxPublisher = new BookingOutboxPublisher(
                outboxEventRepository,
                rabbitTemplate,
                "mentor.events",
                "booking.lifecycle"
        );
    }

    @Test
    void publishPendingEventsSucceedsAndMarksEventPublished() {
        OutboxEvent event = OutboxEvent.of("BookingConfirmed", "100", "{\"bookingId\":100}");
        when(outboxEventRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(event));

        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(4);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(eq("mentor.events"), eq("booking.lifecycle"), eq(event.getPayload()),
                any(MessagePostProcessor.class), any(CorrelationData.class));

        outboxPublisher.publishPendingEvents();

        assertNotNull(event.getPublishedAt());
        assertNull(event.getLastError());
        verify(outboxEventRepository).save(event);
    }

    @Test
    void publishPendingEventsHandlesFailureGracefully() {
        OutboxEvent event = OutboxEvent.of("BookingConfirmed", "100", "{\"bookingId\":100}");
        when(outboxEventRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(event));

        doThrow(new RuntimeException("RabbitMQ connection refused"))
                .when(rabbitTemplate).convertAndSend(eq("mentor.events"), eq("booking.lifecycle"), anyString(),
                        any(MessagePostProcessor.class), any(CorrelationData.class));

        outboxPublisher.publishPendingEvents();

        assertNull(event.getPublishedAt());
        assertEquals(1, event.getPublishAttempts());
        assertTrue(event.getLastError().contains("RabbitMQ connection refused"));
        verify(outboxEventRepository).save(event);
    }
}
