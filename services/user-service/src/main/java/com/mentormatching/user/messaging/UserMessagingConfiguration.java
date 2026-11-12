package com.mentormatching.user.messaging;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topology RabbitMQ của user-service: exchange dùng chung {@code mentor.events} (topic, durable — cùng tham số các
 * service khác) và queue RIÊNG {@code user.account-events} (+ {@code .dlx}/{@code .dead}).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(UserMessagingProperties.class)
@ConditionalOnProperty(name = "user.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class UserMessagingConfiguration {

    public static final String LISTENER_CONTAINER_FACTORY = "userListenerContainerFactory";

    @Bean
    Declarables userTopology(UserMessagingProperties properties) {
        TopicExchange events = new TopicExchange(properties.exchange(), true, false);
        DirectExchange deadLetterExchange = new DirectExchange(properties.deadLetterExchange(), true, false);
        Queue queue = QueueBuilder.durable(properties.queue())
                .deadLetterExchange(properties.deadLetterExchange())
                .deadLetterRoutingKey(properties.deadLetterQueue())
                .build();
        Queue deadLetters = QueueBuilder.durable(properties.deadLetterQueue()).build();
        List<Declarable> declarables = new ArrayList<>(List.of(events, deadLetterExchange, queue, deadLetters,
                BindingBuilder.bind(deadLetters).to(deadLetterExchange).with(properties.deadLetterQueue())));
        properties.routingKeys().forEach(key -> declarables.add(BindingBuilder.bind(queue).to(events).with(key)));
        return new Declarables(declarables);
    }

    /** Lỗi tạm thời: thử lại có backoff; sự kiện sai định dạng: không thử lại, vào dead-letter queue. */
    @Bean(LISTENER_CONTAINER_FACTORY)
    SimpleRabbitListenerContainerFactory userListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory,
            UserMessagingProperties properties) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .configureRetryPolicy(policy -> policy
                        .maxRetries(properties.maxRetries())
                        .delay(Duration.ofSeconds(1))
                        .multiplier(2)
                        .maxDelay(Duration.ofSeconds(10))
                        .predicate(UserMessagingConfiguration::isRetryable))
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build());
        return factory;
    }

    static boolean isRetryable(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof InvalidEventException || cause instanceof AmqpRejectAndDontRequeueException) {
                return false;
            }
        }
        return true;
    }
}
