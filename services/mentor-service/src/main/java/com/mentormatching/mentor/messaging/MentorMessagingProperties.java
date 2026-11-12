package com.mentormatching.mentor.messaging;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mentor.messaging")
public record MentorMessagingProperties(
        boolean enabled,
        String exchange,
        String ratingQueue,
        String ratingRoutingKey,
        String approvedRoutingKey,
        String rejectedRoutingKey,
        Duration confirmTimeout,
        Duration publishDelay
) {
    public MentorMessagingProperties {
        exchange = (exchange == null || exchange.isBlank()) ? "mentor.events" : exchange;
        ratingQueue = (ratingQueue == null || ratingQueue.isBlank()) ? "mentor.rating-events" : ratingQueue;
        ratingRoutingKey = (ratingRoutingKey == null || ratingRoutingKey.isBlank()) ? "mentor.rating" : ratingRoutingKey;
        approvedRoutingKey = (approvedRoutingKey == null || approvedRoutingKey.isBlank()) ? "mentor.approved" : approvedRoutingKey;
        rejectedRoutingKey = (rejectedRoutingKey == null || rejectedRoutingKey.isBlank()) ? "mentor.rejected" : rejectedRoutingKey;
        confirmTimeout = confirmTimeout == null ? Duration.ofSeconds(5) : confirmTimeout;
        publishDelay = publishDelay == null ? Duration.ofSeconds(2) : publishDelay;
    }
}
