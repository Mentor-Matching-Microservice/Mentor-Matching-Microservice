package com.mentormatching.notification.messaging;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.mentormatching.notification.event.InvalidEventException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
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
 * Topology RabbitMQ của notification-service. Chỉ khai báo queue/exchange RIÊNG của service này, cộng với
 * exchange dùng chung {@code mentor.events} với đúng tham số các service khác đang dùng (topic, durable).
 *
 * <pre>
 * mentor.events (topic) --[auth.#, mentor.#, ...]--> notification.domain-events
 *                                                        | lỗi không xử lý được / hết số lần thử
 *                                                        v
 *                        notification.domain-events.dlx --> notification.domain-events.dead
 * </pre>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MessagingProperties.class)
@ConditionalOnProperty(name = "notification.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class MessagingConfiguration {

    public static final String LISTENER_CONTAINER_FACTORY = "notificationListenerContainerFactory";

    @Bean
    Declarables notificationTopology(MessagingProperties properties) {
        TopicExchange events = new TopicExchange(properties.exchange(), true, false);
        DirectExchange deadLetterExchange = new DirectExchange(properties.deadLetterExchange(), true, false);
        Queue queue = QueueBuilder.durable(properties.queue())
                .deadLetterExchange(properties.deadLetterExchange())
                .deadLetterRoutingKey(properties.deadLetterQueue())
                .build();
        Queue deadLetters = QueueBuilder.durable(properties.deadLetterQueue()).build();

        List<org.springframework.amqp.core.Declarable> declarables = new ArrayList<>(
                List.of(events, deadLetterExchange, queue, deadLetters,
                        BindingBuilder.bind(deadLetters).to(deadLetterExchange).with(properties.deadLetterQueue())));
        for (String routingKey : properties.routingKeys()) {
            Binding binding = BindingBuilder.bind(queue).to(events).with(routingKey);
            declarables.add(binding);
        }
        return new Declarables(declarables);
    }

    /**
     * Lỗi tạm thời được thử lại với backoff tăng dần; sự kiện sai định dạng không thử lại.
     * Hết số lần thử hoặc sự kiện sai → reject không requeue → dead-letter queue.
     */
    @Bean(LISTENER_CONTAINER_FACTORY)
    SimpleRabbitListenerContainerFactory notificationListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory,
            MessagingProperties properties) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .configureRetryPolicy(policy -> policy
                        .maxRetries(properties.maxRetries())
                        .delay(Duration.ofSeconds(1))
                        .multiplier(2)
                        .maxDelay(Duration.ofSeconds(10))
                        .predicate(MessagingConfiguration::isRetryable))
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
