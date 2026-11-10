package com.mentormatching.review.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class HttpUserLookupClient implements UserLookupClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public HttpUserLookupClient(
            @Value("${review.services.user-service-url:http://localhost:8082}") String userServiceUrl,
            ObjectMapper objectMapper) {
        this.restClient = RestClient.builder().baseUrl(userServiceUrl).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public String getUserFullName(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        try {
            String rawJson = restClient.get()
                    .uri("/internal/v1/users/{userId}/summary", userId)
                    .retrieve()
                    .body(String.class);

            if (rawJson != null) {
                JsonNode root = objectMapper.readTree(rawJson);
                JsonNode dataNode = root.has("data") ? root.get("data") : root;
                if (dataNode.has("fullName")) {
                    return dataNode.get("fullName").asText();
                }
            }
        } catch (Exception e) {
            log.debug("Failed to fetch user summary for userId {}: {}", userId, e.getMessage());
        }
        return "User " + userId;
    }
}
