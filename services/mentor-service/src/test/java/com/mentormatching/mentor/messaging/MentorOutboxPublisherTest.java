package com.mentormatching.mentor.messaging;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.mentormatching.mentor.entity.OutboxEvent;
import com.mentormatching.mentor.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MentorOutboxPublisherTest {

    @Mock
    OutboxEventRepository outboxEventRepository;

    @Mock
    RabbitTemplate rabbitTemplate;

    @Mock
    PlatformTransactionManager transactionManager;

    ObjectMapper objectMapper = new ObjectMapper();
    MentorMessagingProperties properties = new MentorMessagingProperties(
            true, "mentor.events", "mentor.rating-events", "mentor.rating", "mentor.approved", "mentor.rejected", null, null
    );

    MentorOutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new MentorOutboxPublisher(
                outboxEventRepository,
                rabbitTemplate,
                properties,
                transactionManager,
                objectMapper
        );
    }

    @Test
    void publishBatch_success() {
        OutboxEvent event = new OutboxEvent(
                "MentorApproved",
                "MENTOR",
                "1",
                "{\"mentorId\":1,\"userId\":\"123e4567-e89b-12d3-a456-426614174000\"}"
        );

        when(outboxEventRepository.findPendingEvents(any(Pageable.class))).thenReturn(List.of(event));

        doAnswer(invocation -> {
            CorrelationData cd = invocation.getArgument(4);
            CorrelationData.Confirm confirm = new CorrelationData.Confirm(true, null);
            cd.getFuture().complete(confirm);
            return null;
        }).when(rabbitTemplate).convertAndSend(eq("mentor.events"), eq("mentor.approved"), any(String.class), any(MessagePostProcessor.class), any(CorrelationData.class));

        int sent = publisher.publishBatch();

        assertThat(sent).isEqualTo(1);
        assertThat(event.getPublishedAt()).isNotNull();
        verify(outboxEventRepository).save(event);
    }

    @Test
    void publishBatch_brokerRejects_recordsError() {
        OutboxEvent event = new OutboxEvent(
                "MentorApproved",
                "MENTOR",
                "1",
                "{\"mentorId\":1,\"userId\":\"123e4567-e89b-12d3-a456-426614174000\"}"
        );

        when(outboxEventRepository.findPendingEvents(any(Pageable.class))).thenReturn(List.of(event));

        doAnswer(invocation -> {
            CorrelationData cd = invocation.getArgument(4);
            CorrelationData.Confirm confirm = new CorrelationData.Confirm(false, "Nack");
            cd.getFuture().complete(confirm);
            return null;
        }).when(rabbitTemplate).convertAndSend(any(), any(), any(String.class), any(MessagePostProcessor.class), any(CorrelationData.class));

        int sent = publisher.publishBatch();

        assertThat(sent).isEqualTo(0);
        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getPublishAttempts()).isEqualTo(1);
        verify(outboxEventRepository).save(event);
    }

    @Test
    void publishPending_runsSuccessfully() {
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new SimpleTransactionStatus());
        when(outboxEventRepository.findPendingEvents(any(Pageable.class))).thenReturn(List.of());

        publisher.publishPending();

        verify(outboxEventRepository).findPendingEvents(any(Pageable.class));
    }
}
