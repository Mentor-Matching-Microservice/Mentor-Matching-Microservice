package com.mentormatching.mentor.messaging;

import java.util.ArrayList;
import java.util.List;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(MentorMessagingProperties.class)
@ConditionalOnProperty(name = "mentor.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class MentorMessagingConfiguration {

    public static final String LISTENER_CONTAINER_FACTORY = "mentorListenerContainerFactory";

    @Bean
    Declarables mentorTopology(MentorMessagingProperties properties) {
        TopicExchange exchange = new TopicExchange(properties.exchange(), true, false);
        Queue ratingQueue = QueueBuilder.durable(properties.ratingQueue()).build();
        List<Declarable> declarables = new ArrayList<>(List.of(
                exchange,
                ratingQueue,
                BindingBuilder.bind(ratingQueue).to(exchange).with(properties.ratingRoutingKey())
        ));
        return new Declarables(declarables);
    }

    @Bean(LISTENER_CONTAINER_FACTORY)
    SimpleRabbitListenerContainerFactory mentorListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
