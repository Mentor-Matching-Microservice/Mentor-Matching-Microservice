package com.mentormatching.booking.client;

import com.mentormatching.booking.client.dto.UserSummaryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class HttpUserLookupClient implements UserLookupClient {

    private final RestClient restClient;
    private final String userServiceUrl;

    public HttpUserLookupClient(@Value("${booking.services.user-service-url:http://localhost:8082}") String userServiceUrl) {
        this.userServiceUrl = userServiceUrl;
        this.restClient = RestClient.builder().baseUrl(userServiceUrl).build();
    }

    @Override
    public UserSummaryResponse getUserSummary(String userId) {
        try {
            return restClient.get()
                    .uri("/internal/v1/users/{userId}/summary", userId)
                    .retrieve()
                    .body(UserSummaryResponse.class);
        } catch (Exception e) {
            log.warn("Failed to fetch user summary from user-service for userId {}: {}", userId, e.getMessage());
            return UserSummaryResponse.builder()
                    .userId(userId)
                    .fullName("Learner " + (userId != null && userId.length() > 6 ? userId.substring(0, 6) : userId))
                    .email("learner@example.com")
                    .phone("0900000000")
                    .build();
        }
    }
}

