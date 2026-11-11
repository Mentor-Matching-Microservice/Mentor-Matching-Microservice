package com.mentormatching.mentor.client;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Client gọi media-service cho private verification media và signed URLs.
 */
public class MediaServiceClient {

    private static final Logger log = LoggerFactory.getLogger(MediaServiceClient.class);

    private final RestClient client;

    public MediaServiceClient(RestClient client) {
        this.client = client;
    }

    public record VerifyAndAttachRequest(
            String mediaAssetId,
            String uploadedByUserId,
            String purpose,
            String consumer,
            String referenceId) {}

    public record UsageResponse(
            String mediaAssetId,
            String uploadedByUserId,
            String purpose,
            String status,
            String consumer,
            String referenceId,
            String usageKey) {}

    public record PrivateDeliveryUrlsRequest(List<String> mediaAssetIds) {}

    public record PrivateDeliveryUrlResponse(String mediaAssetId, String url, String expiresAt) {}

    public record PrivateDeliveryUrlsResponse(List<PrivateDeliveryUrlResponse> urls) {}

    /**
     * Xác minh tài nguyên PRIVATE với purpose = VERIFICATION thuộc sở hữu của user.
     */
    public boolean verifyAndAttach(String mediaAssetId, UUID uploadedByUserId, String referenceId) {
        if (mediaAssetId == null || mediaAssetId.isBlank()) {
            return true;
        }
        try {
            var request = new VerifyAndAttachRequest(
                    mediaAssetId,
                    uploadedByUserId.toString(),
                    "VERIFICATION",
                    "mentor-service",
                    referenceId != null ? referenceId : UUID.randomUUID().toString()
            );
            client.post()
                    .uri("/internal/v1/media/verify-and-attach")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.warn("Failed to verify media asset {}: {}", mediaAssetId, e.getMessage());
            return false;
        }
    }

    /**
     * Lấy short-lived private delivery URLs cho danh sách mediaAssetIds.
     */
    public Map<String, String> getPrivateDeliveryUrls(List<String> mediaAssetIds) {
        if (mediaAssetIds == null || mediaAssetIds.isEmpty()) {
            return Map.of();
        }
        var validIds = mediaAssetIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        if (validIds.isEmpty()) {
            return Map.of();
        }

        try {
            var response = client.post()
                    .uri("/internal/v1/media/private-delivery-urls")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new PrivateDeliveryUrlsRequest(validIds))
                    .retrieve()
                    .body(PrivateDeliveryUrlsResponse.class);

            if (response != null && response.urls() != null) {
                return response.urls().stream()
                        .filter(u -> u.mediaAssetId() != null && u.url() != null)
                        .collect(Collectors.toMap(
                                PrivateDeliveryUrlResponse::mediaAssetId,
                                PrivateDeliveryUrlResponse::url,
                                (a, b) -> a
                        ));
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve private delivery URLs: {}", e.getMessage());
        }
        return Map.of();
    }
}
