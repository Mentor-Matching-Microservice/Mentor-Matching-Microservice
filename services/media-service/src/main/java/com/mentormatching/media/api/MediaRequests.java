package com.mentormatching.media.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public final class MediaRequests {
    private MediaRequests() {}
    public record CreateIntent(@NotBlank String purpose, @NotBlank String resourceType, @NotBlank String originalFilename,
                               @NotBlank String mimeType, @NotNull @Positive Long fileSizeBytes) {}
    public record Confirm(@NotBlank String assetId) {}
    public record AssetResponse(String id, String status, String purpose, String resourceType, String accessType,
                                String publicId, String secureUrl, long size, Integer width, Integer height,
                                String createdAt) {}
    public record UploadIntent(String assetId, String uploadUrl, String publicId, String resourceType,
                               String deliveryType, long timestamp, String signature, String apiKey, InstantExpiry expiresAt) {}
    public record InstantExpiry(String at) {}
}
