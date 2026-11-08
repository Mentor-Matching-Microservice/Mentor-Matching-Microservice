package com.mentormatching.booking.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "booking.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class BookingMessagingConfiguration {

    @Bean
    TopicExchange mentorEventsExchange(@Value("${booking.messaging.exchange:mentor.events}") String name) {
        return new TopicExchange(name, true, false);
    }

    @Bean
    DirectExchange bookingDeadLetterExchange() {
        return new DirectExchange("mentor.events.dlx", true, false);
    }

    @Bean
    Queue bookingPaymentEventsQueue(
            @Value("${booking.messaging.payment-events-queue:booking.payment-events}") String name) {
        return QueueBuilder.durable(name)
                .deadLetterExchange("mentor.events.dlx")
                .deadLetterRoutingKey("payment.events.dead")
                .build();
    }

    @Bean
    Binding bookingPaymentEventsBinding(Queue bookingPaymentEventsQueue, TopicExchange mentorEventsExchange) {
        return BindingBuilder.bind(bookingPaymentEventsQueue).to(mentorEventsExchange).with("payment.#");
    }

    @Bean
    Queue bookingPaymentDeadLetters() {
        return QueueBuilder.durable("booking.payment-events.dead").build();
    }

    @Bean
    Binding bookingPaymentDeadLetterBinding(Queue bookingPaymentDeadLetters, DirectExchange bookingDeadLetterExchange) {
        return BindingBuilder.bind(bookingPaymentDeadLetters)
                .to(bookingDeadLetterExchange).with("payment.events.dead");
    }
}
