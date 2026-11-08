package com.mentormatching.payment.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "payment.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentMessagingConfiguration {
    @Bean
    TopicExchange mentorEventsExchange(@Value("${payment.messaging.exchange:mentor.events}") String name) {
        return new TopicExchange(name, true, false);
    }

    @Bean
    DirectExchange paymentDeadLetterExchange() {
        return new DirectExchange("mentor.events.dlx", true, false);
    }

    @Bean
    Queue paymentBookingLifecycleQueue(
            @Value("${payment.messaging.booking-events-queue:payment.booking-lifecycle}") String name) {
        return QueueBuilder.durable(name)
                .deadLetterExchange("mentor.events.dlx")
                .deadLetterRoutingKey("booking.lifecycle.dead")
                .build();
    }

    @Bean
    Binding paymentBookingLifecycleBinding(Queue paymentBookingLifecycleQueue, TopicExchange mentorEventsExchange) {
        return BindingBuilder.bind(paymentBookingLifecycleQueue).to(mentorEventsExchange).with("booking.lifecycle");
    }

    @Bean
    Queue paymentBookingLifecycleDeadLetters() {
        return QueueBuilder.durable("payment.booking-lifecycle.dead").build();
    }

    @Bean
    Binding paymentBookingLifecycleDeadLetterBinding(Queue paymentBookingLifecycleDeadLetters,
                                                       DirectExchange paymentDeadLetterExchange) {
        return BindingBuilder.bind(paymentBookingLifecycleDeadLetters)
                .to(paymentDeadLetterExchange).with("booking.lifecycle.dead");
    }

    @Bean
    Queue paymentEventsQueue() {
        return QueueBuilder.durable("payment.events").build();
    }

    @Bean
    Binding paymentEventsBinding(Queue paymentEventsQueue, TopicExchange mentorEventsExchange) {
        return BindingBuilder.bind(paymentEventsQueue).to(mentorEventsExchange).with("payment.#");
    }
}
