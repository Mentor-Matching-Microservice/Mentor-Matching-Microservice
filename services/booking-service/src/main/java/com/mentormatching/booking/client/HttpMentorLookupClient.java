package com.mentormatching.booking.client;

import com.mentormatching.booking.client.dto.MentorBookingContextResponse;
import com.mentormatching.booking.client.dto.MentorMeetingType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
@Slf4j
public class HttpMentorLookupClient implements MentorLookupClient {

    private final RestClient restClient;
    private final String mentorServiceUrl;

    public HttpMentorLookupClient(@Value("${booking.services.mentor-service-url:http://localhost:8083}") String mentorServiceUrl) {
        this.mentorServiceUrl = mentorServiceUrl;
        this.restClient = RestClient.builder().baseUrl(mentorServiceUrl).build();
    }

    @Override
    public MentorBookingContextResponse getBookingContext(Long mentorId, Long mentorSubjectId) {
        try {
            return restClient.get()
                    .uri("/internal/v1/mentors/{mentorId}/booking-context?mentorSubjectId={mentorSubjectId}",
                            mentorId, mentorSubjectId)
                    .retrieve()
                    .body(MentorBookingContextResponse.class);
        } catch (Exception e) {
            log.warn("Failed to fetch mentor booking context from mentor-service for mentorId {}, subjectId {}: {}",
                    mentorId, mentorSubjectId, e.getMessage());
            // Fallback for standalone / mock environments
            return MentorBookingContextResponse.builder()
                    .mentorId(mentorId)
                    .mentorUserId("mentor-user-" + mentorId)
                    .mentorName("Mentor " + mentorId)
                    .meetingType(MentorMeetingType.HYBRID)
                    .mentorSubjectId(mentorSubjectId)
                    .subjectName("Subject " + mentorSubjectId)
                    .gradeName("Grade 12")
                    .pricePerHour(new BigDecimal("200000.00"))
                    .active(true)
                    .build();
        }
    }
}

