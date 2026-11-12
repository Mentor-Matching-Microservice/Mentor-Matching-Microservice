package com.mentormatching.review.service;

import com.mentormatching.review.entity.OutboxEvent;
import com.mentormatching.review.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReviewOutboxPublisherTest {

    private OutboxEventRepository outboxEventRepository;
    private RabbitTemplate rabbitTemplate;
    private ReviewOutboxPublisher outboxPublisher;

    @BeforeEach
    void setUp() {
        outboxEventRepository = mock(OutboxEventRepository.class);
        rabbitTemplate = mock(RabbitTemplate.class);
        outboxPublisher = new ReviewOutboxPublisher(
                outboxEventRepository,
                rabbitTemplate,
                "mentor.events",
                "mentor.rating"
        );
    }

    @Test
    void publishPendingEventsSucceedsAndMarksEventPublished() {
        OutboxEvent event = OutboxEvent.of("MentorRatingUpdated", "5", "{\"mentorId\":5,\"averageRating\":4.8}");
        when(outboxEventRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(event));

        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(4);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(eq("mentor.events"), eq("mentor.rating"), eq(event.getPayload()),
                any(MessagePostProcessor.class), any(CorrelationData.class));

        outboxPublisher.publishPendingEvents();

        assertNotNull(event.getPublishedAt());
        assertNull(event.getLastError());
        verify(outboxEventRepository).save(event);
    }

    @Test
    void publishPendingEventsHandlesFailureGracefully() {
        OutboxEvent event = OutboxEvent.of("MentorRatingUpdated", "5", "{\"mentorId\":5,\"averageRating\":4.8}");
        when(outboxEventRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(event));

        doThrow(new RuntimeException("RabbitMQ broker unavailable"))
                .when(rabbitTemplate).convertAndSend(eq("mentor.events"), eq("mentor.rating"), anyString(),
                        any(MessagePostProcessor.class), any(CorrelationData.class));

        outboxPublisher.publishPendingEvents();

        assertNull(event.getPublishedAt());
        assertEquals(1, event.getPublishAttempts());
        assertTrue(event.getLastError().contains("RabbitMQ broker unavailable"));
        verify(outboxEventRepository).save(event);
    }
}
