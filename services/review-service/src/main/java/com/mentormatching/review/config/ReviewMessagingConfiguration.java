package com.mentormatching.review.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "review.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class ReviewMessagingConfiguration {

    @Bean
    TopicExchange mentorEventsExchange(@Value("${review.messaging.exchange:mentor.events}") String name) {
        return new TopicExchange(name, true, false);
    }

    @Bean
    DirectExchange reviewDeadLetterExchange() {
        return new DirectExchange("mentor.events.dlx", true, false);
    }
}
