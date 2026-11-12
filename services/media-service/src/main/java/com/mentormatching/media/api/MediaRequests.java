package com.mentormatching.media.api;

import java.time.Instant;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class MediaRequests {
    private MediaRequests() {}
    public record CreateIntent(@NotBlank String purpose, @NotBlank String resourceType, @NotBlank String originalFilename,
                               @NotBlank String mimeType, @NotNull @Positive Long fileSizeBytes) {}
    public record Confirm(@NotBlank String assetId) {}
    public record VerifyAndAttach(@NotBlank String mediaAssetId, @NotBlank String uploadedByUserId,
                                  @NotBlank String purpose, @NotBlank String consumer,
                                  @NotBlank @Size(max = 128) String referenceId) {}
    public record PrivateDeliveryUrls(@NotEmpty @Size(max = 50) List<@NotBlank String> mediaAssetIds) {}
    public record UsageResponse(String mediaAssetId, String uploadedByUserId, String purpose,
                                String status, String consumer, String referenceId, String usageKey) {}
    public record PrivateDeliveryUrlResponse(String mediaAssetId, String url, Instant expiresAt) {}
    public record PrivateDeliveryUrlsResponse(List<PrivateDeliveryUrlResponse> urls) {}
    public record AssetResponse(String id, String status, String purpose, String resourceType, String accessType,
                                String publicId, String secureUrl, long size, Integer width, Integer height,
                                String createdAt) {}
    public record UploadIntent(String assetId, String uploadUrl, String publicId, String resourceType,
                               String deliveryType, long timestamp, String signature, String apiKey, InstantExpiry expiresAt) {}
    public record InstantExpiry(String at) {}
}
