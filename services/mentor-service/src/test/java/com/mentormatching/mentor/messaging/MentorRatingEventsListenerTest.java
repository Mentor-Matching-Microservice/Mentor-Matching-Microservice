package com.mentormatching.mentor.messaging;

import com.mentormatching.mentor.service.ApplyMentorRatingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MentorRatingEventsListenerTest {

    @Mock
    ApplyMentorRatingService applyMentorRatingService;

    ObjectMapper objectMapper = new ObjectMapper();

    MentorRatingEventsListener listener;

    @BeforeEach
    void setUp() {
        listener = new MentorRatingEventsListener(objectMapper, applyMentorRatingService);
    }

    @Test
    void onMessage_validRatingEvent_appliesRating() {
        String json = """
                {
                    "eventId": "evt-123",
                    "eventType": "MentorRatingUpdated",
                    "mentorId": 5,
                    "averageRating": 4.85,
                    "totalReviews": 20,
                    "version": 4
                }
                """;
        Message message = new Message(json.getBytes(), new MessageProperties());

        listener.onMessage(message);

        verify(applyMentorRatingService).apply(5L, 4.85, 20L, 4L);
    }

    @Test
    void onMessage_missingMentorId_ignores() {
        String json = """
                {
                    "eventId": "evt-123",
                    "eventType": "MentorRatingUpdated",
                    "averageRating": 4.85
                }
                """;
        Message message = new Message(json.getBytes(), new MessageProperties());

        listener.onMessage(message);

        verifyNoInteractions(applyMentorRatingService);
    }

    @Test
    void onMessage_invalidJson_handlesGracefully() {
        Message message = new Message("not-json".getBytes(), new MessageProperties());

        listener.onMessage(message);

        verifyNoInteractions(applyMentorRatingService);
    }
}
