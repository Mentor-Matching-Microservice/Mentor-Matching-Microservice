package com.mentormatching.review.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentormatching.review.client.dto.BookingReviewEligibilityResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class HttpBookingLookupClient implements BookingLookupClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public HttpBookingLookupClient(
            @Value("${review.services.booking-service-url:http://localhost:8084}") String bookingServiceUrl,
            ObjectMapper objectMapper) {
        this.restClient = RestClient.builder().baseUrl(bookingServiceUrl).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public BookingReviewEligibilityResponse getReviewEligibility(Long bookingId) {
        try {
            String rawJson = restClient.get()
                    .uri("/internal/v1/bookings/{bookingId}/review-eligibility", bookingId)
                    .retrieve()
                    .body(String.class);

            if (rawJson != null) {
                JsonNode root = objectMapper.readTree(rawJson);
                JsonNode dataNode = root.has("data") ? root.get("data") : root;
                return objectMapper.treeToValue(dataNode, BookingReviewEligibilityResponse.class);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch booking review eligibility from booking-service for bookingId {}: {}",
                    bookingId, e.getMessage());
        }

        // Return empty or ineligible response when service is unreachable
        return BookingReviewEligibilityResponse.builder()
                .bookingId(bookingId)
                .eligibleForReview(false)
                .status("UNKNOWN")
                .build();
    }
}
