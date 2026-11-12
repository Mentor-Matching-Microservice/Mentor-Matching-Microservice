package com.mentormatching.mentor.messaging;

import com.mentormatching.mentor.service.ApplyMentorRatingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "mentor.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class MentorRatingEventsListener {

    private static final Logger log = LoggerFactory.getLogger(MentorRatingEventsListener.class);

    private final ObjectMapper objectMapper;
    private final ApplyMentorRatingService applyMentorRatingService;

    public MentorRatingEventsListener(ObjectMapper objectMapper, ApplyMentorRatingService applyMentorRatingService) {
        this.objectMapper = objectMapper;
        this.applyMentorRatingService = applyMentorRatingService;
    }

    @RabbitListener(queues = "${mentor.messaging.rating-queue:mentor.rating-events}",
            containerFactory = MentorMessagingConfiguration.LISTENER_CONTAINER_FACTORY)
    public void onMessage(Message message) {
        try {
            JsonNode node = objectMapper.readTree(message.getBody());
            if (node == null || !node.isObject()) {
                log.warn("Received invalid non-object message on rating queue");
                return;
            }

            if (!node.has("mentorId") || node.get("mentorId").isNull()) {
                log.warn("Received MentorRatingUpdated event without mentorId: {}", node);
                return;
            }

            Long mentorId = node.get("mentorId").asLong();
            Double averageRating = node.has("averageRating") && !node.get("averageRating").isNull()
                    ? node.get("averageRating").asDouble()
                    : null;
            Long totalReviews = node.has("totalReviews") ? node.get("totalReviews").asLong(0L) : 0L;
            long version = node.has("version") ? node.get("version").asLong(0L) : 0L;

            boolean updated = applyMentorRatingService.apply(mentorId, averageRating, totalReviews, version);
            log.info("Processed MentorRatingUpdated event mentorId={} rating={} totalReviews={} version={} applied={}",
                    mentorId, averageRating, totalReviews, version, updated);
        } catch (Exception ex) {
            log.error("Failed to process MentorRatingUpdated event: {}", ex.getMessage(), ex);
        }
    }
}
