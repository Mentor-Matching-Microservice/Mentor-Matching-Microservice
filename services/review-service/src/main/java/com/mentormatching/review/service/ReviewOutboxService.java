package com.mentormatching.review.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentormatching.review.entity.OutboxEvent;
import com.mentormatching.review.event.MentorRatingUpdatedEvent;
import com.mentormatching.review.event.ReviewCreatedEvent;
import com.mentormatching.review.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewOutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void recordReviewCreated(Long reviewId, Long bookingId, String studentUserId, Long mentorId, Integer rating) {
        String eventId = UUID.randomUUID().toString();
        ReviewCreatedEvent event = new ReviewCreatedEvent(
                eventId,
                "ReviewCreated",
                Instant.now(),
                reviewId,
                bookingId,
                studentUserId,
                mentorId,
                rating
        );

        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.of("ReviewCreated", String.valueOf(reviewId), payload);
            outboxEventRepository.save(outboxEvent);
            log.info("Recorded OutboxEvent ReviewCreated: reviewId={}, mentorId={}, rating={}",
                    reviewId, mentorId, rating);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ReviewCreatedEvent: {}", e.getMessage(), e);
            throw new IllegalStateException("Could not serialize ReviewCreatedEvent", e);
        }
    }

    public void recordMentorRatingUpdated(Long mentorId, Double averageRating, Long totalReviews) {
        String eventId = UUID.randomUUID().toString();
        MentorRatingUpdatedEvent event = new MentorRatingUpdatedEvent(
                eventId,
                "MentorRatingUpdated",
                Instant.now(),
                mentorId,
                averageRating,
                totalReviews,
                System.currentTimeMillis()
        );

        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.of("MentorRatingUpdated", String.valueOf(mentorId), payload);
            outboxEventRepository.save(outboxEvent);
            log.info("Recorded OutboxEvent MentorRatingUpdated: mentorId={}, averageRating={}, totalReviews={}",
                    mentorId, averageRating, totalReviews);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize MentorRatingUpdatedEvent: {}", e.getMessage(), e);
            throw new IllegalStateException("Could not serialize MentorRatingUpdatedEvent", e);
        }
    }
}
