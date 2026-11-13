package com.mentormatching.media.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.mentormatching.media.config.MediaProperties;
import com.mentormatching.media.domain.MediaAsset;
import com.mentormatching.media.domain.MediaAssetRepository;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MediaService {
    private static final Set<String> PURPOSES = Set.of("AVATAR", "ACHIEVEMENT", "REVIEW_ATTACHMENT", "VERIFICATION");
    private static final Set<String> RESOURCE_TYPES = Set.of("image", "video", "raw");
    private static final Set<String> ALLOWED_CONSUMERS = Set.of("user-service", "mentor-service", "review-service");

    private final MediaAssetRepository assets;
    private final MongoOperations mongo;
    private final CloudinaryService cloud;
    private final MediaProperties config;

    public MediaService(MediaAssetRepository assets, MongoOperations mongo, CloudinaryService cloud,
            MediaProperties config) {
        this.assets = assets;
        this.mongo = mongo;
        this.cloud = cloud;
        this.config = config;
    }

    public Map<String, Object> create(String owner, String purpose, String resourceType,
            String filename, String mime, long size) {
        purpose = purpose.toUpperCase(Locale.ROOT);
        resourceType = resourceType.toLowerCase(Locale.ROOT);
        mime = mime.toLowerCase(Locale.ROOT);
        if (!PURPOSES.contains(purpose) || !RESOURCE_TYPES.contains(resourceType)) {
            throw bad("Unsupported media purpose or resource type");
        }
        if (size < 1 || size > config.maxFileSizeBytes()) {
            throw bad("File size exceeds the allowed limit");
        }
        if (purpose.equals("AVATAR") && !resourceType.equals("image")) {
            throw bad("Avatar uploads must be images");
        }
        if (!supportedMime(resourceType, mime)) {
            throw bad("File MIME type is not allowed for this resource type");
        }
        String access = purpose.equals("VERIFICATION") ? "PRIVATE" : "PUBLIC";
        String publicId = config.uploadFolder() + "/users/" + owner + "/" + purpose.toLowerCase(Locale.ROOT)
                + "/" + UUID.randomUUID().toString().replace("-", "");
        MediaAsset asset = assets.save(new MediaAsset(owner, purpose, resourceType, access, publicId,
                filename, mime, size, Instant.now().plus(config.uploadIntentTtl())));

        if (!cloud.isConfigured()) {
            java.util.Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("assetId", asset.getId());
            response.put("uploadUrl", "http://localhost:5173/mock/media/upload");
            response.put("publicId", asset.getPublicId());
            response.put("resourceType", asset.getResourceType());
            response.put("deliveryType", access.equals("PRIVATE") ? "authenticated" : "upload");
            response.put("fields", java.util.Map.of("public_id", asset.getPublicId()));
            response.put("expiresAt", asset.getExpiresAt());
            return response;
        }

        return cloud.uploadIntent(asset);
    }

    public MediaAsset confirm(String id, String owner, String assetId) {
        MediaAsset asset = owned(id, owner);
        if (asset.getStatus().equals("READY")) {
            if (!java.util.Objects.equals(asset.getAssetId(), assetId)) {
                throw bad("Confirmed asset id differs");
            }
            return asset;
        }
        if (!asset.getStatus().equals("PENDING") || asset.getExpiresAt() == null
                || !asset.getExpiresAt().isAfter(Instant.now())) {
            throw bad("Upload intent is expired or no longer confirmable");
        }
        if (!cloud.isConfigured()) {
            String mockDeliveryUrl = "https://images.unsplash.com/photo-1544717305-2782549b5136?auto=format&fit=crop&q=80&w=600";
            asset.ready(assetId != null && !assetId.isBlank() ? assetId : "mock-asset-" + UUID.randomUUID(),
                    mockDeliveryUrl, asset.getSize(), 800, 600, 1L, "png", "mock-etag");
            return assets.save(asset);
        }
        Map<String, Object> metadata = cloud.verify(asset, assetId);
        if (metadata == null || !(metadata.get("size") instanceof Number size)
                || !(metadata.get("assetId") instanceof String verifiedId)
                || !(metadata.get("secureUrl") instanceof String secureUrl)
                || !(metadata.get("format") instanceof String format)
                || !(metadata.get("etag") instanceof String etag)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Cloudinary returned incomplete asset metadata");
        }
        asset.ready(verifiedId, secureUrl, size.longValue(), number(metadata.get("width")),
                number(metadata.get("height")), longNumber(metadata.get("version")), format, etag);
        return assets.save(asset);
    }

    public MediaAsset verifyAndAttach(String mediaAssetId, String uploadedByUserId, String purpose,
            String consumer, String referenceId) {
        String ownerId = canonicalUuid(uploadedByUserId, "uploadedByUserId");
        String normalizedPurpose = purpose.toUpperCase(Locale.ROOT);
        String normalizedConsumer = consumer.toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONSUMERS.contains(normalizedConsumer)) {
            throw bad("Unsupported media consumer");
        }
        String normalizedReferenceId = canonicalUuid(referenceId, "referenceId");

        MediaAsset asset = assets.findById(mediaAssetId).orElseThrow(() -> notFound());
        if (!asset.getOwnerId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Media asset does not belong to uploadedByUserId");
        }
        if (!asset.getPurpose().equals(normalizedPurpose)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Media asset purpose does not match the requested purpose");
        }
        if (!asset.getStatus().equals("READY")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only READY media assets can be attached");
        }

        String usageKey = normalizedConsumer + ":" + normalizedReferenceId;
        Query query = Query.query(Criteria.where("_id").is(mediaAssetId)
                .and("ownerId").is(ownerId)
                .and("purpose").is(normalizedPurpose)
                .and("status").is("READY"));
        Update update = new Update().addToSet("usageKeys", usageKey).set("updatedAt", Instant.now());
        MediaAsset attached = mongo.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(true), MediaAsset.class);
        if (attached == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Media asset changed while it was being attached");
        }
        return attached;
    }

    public List<PrivateDeliveryUrl> privateDeliveryUrls(List<String> mediaAssetIds) {
        if (mediaAssetIds == null || mediaAssetIds.isEmpty() || mediaAssetIds.size() > 50
                || new HashSet<>(mediaAssetIds).size() != mediaAssetIds.size()) {
            throw bad("Provide between 1 and 50 distinct media asset IDs");
        }
        List<MediaAsset> verifiedAssets = new ArrayList<>(mediaAssetIds.size());
        for (String id : mediaAssetIds) {
            MediaAsset asset = assets.findById(id).orElseThrow(() -> notFound());
            if (!asset.getStatus().equals("READY")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Only READY media assets can be delivered");
            }
            if (!asset.getAccessType().equals("PRIVATE")) {
                throw bad("Private delivery URLs can only be requested for PRIVATE assets");
            }
            verifiedAssets.add(asset);
        }

        Instant expiresAt = Instant.ofEpochSecond(Instant.now().plus(config.privateUrlTtl()).getEpochSecond());
        if (!cloud.isConfigured()) {
            return verifiedAssets.stream()
                    .map(asset -> new PrivateDeliveryUrl(asset.getId(),
                            asset.getSecureUrl() != null && !asset.getSecureUrl().isBlank()
                                    ? asset.getSecureUrl()
                                    : "https://images.unsplash.com/photo-1544717305-2782549b5136?auto=format&fit=crop&q=80&w=600",
                            expiresAt))
                    .toList();
        }
        return verifiedAssets.stream()
                .map(asset -> new PrivateDeliveryUrl(asset.getId(), cloud.privateUrl(asset, expiresAt), expiresAt))
                .toList();
    }

    public String signedUrl(String id, String owner) {
        MediaAsset asset = owned(id, owner);
        if (!asset.getStatus().equals("READY")) {
            throw bad("Media asset is not ready");
        }
        if (!cloud.isConfigured()) {
            return asset.getSecureUrl() != null ? asset.getSecureUrl() : "https://images.unsplash.com/photo-1544717305-2782549b5136?auto=format&fit=crop&q=80&w=600";
        }
        return asset.getAccessType().equals("PRIVATE") ? cloud.privateUrl(asset) : asset.getSecureUrl();
    }

    public String internalSignedUrl(String id) {
        MediaAsset asset = assets.findById(id).orElseThrow(() -> notFound());
        if (!asset.getStatus().equals("READY")) {
            throw bad("Media asset is not ready");
        }
        if (!cloud.isConfigured()) {
            return asset.getSecureUrl() != null ? asset.getSecureUrl() : "https://images.unsplash.com/photo-1544717305-2782549b5136?auto=format&fit=crop&q=80&w=600";
        }
        return asset.getAccessType().equals("PRIVATE") ? cloud.privateUrl(asset) : asset.getSecureUrl();
    }

    public void delete(String id, String owner) {
        MediaAsset asset = owned(id, owner);
        if (asset.getStatus().equals("DELETED")) {
            return;
        }
        if (cloud.isConfigured()) {
            cloud.delete(asset);
        }
        asset.delete("DELETED");
        assets.save(asset);
    }

    @Scheduled(fixedDelayString = "${media.cloudinary.cleanup-delay-ms:3600000}")
    public void cleanupExpiredIntents() {
        if (!cloud.isConfigured()) {
            return;
        }
        Instant olderThan = Instant.now().minus(24, ChronoUnit.HOURS);
        for (MediaAsset asset : assets.findTop100ByStatusAndCreatedAtBefore("PENDING", olderThan)) {
            try {
                cloud.delete(asset);
                asset.delete("EXPIRED");
                assets.save(asset);
            } catch (RuntimeException ignored) {
                // Keep the intent pending so a later run retries the Cloudinary cleanup.
            }
        }
    }

    private MediaAsset owned(String id, String owner) {
        return assets.findByIdAndOwnerId(id, owner).orElseThrow(() -> notFound());
    }

    private static boolean supportedMime(String resourceType, String mime) {
        return switch (resourceType) {
            case "image" -> Set.of("image/jpeg", "image/png", "image/webp", "image/gif").contains(mime);
            case "video" -> Set.of("video/mp4", "video/webm").contains(mime);
            case "raw" -> mime.equals("application/pdf");
            default -> false;
        };
    }

    private static String canonicalUuid(String value, String field) {
        try {
            return UUID.fromString(value).toString();
        } catch (RuntimeException exception) {
            throw bad(field + " must be a UUID");
        }
    }

    private static Integer number(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private static Long longNumber(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Media asset not found");
    }

    private static ResponseStatusException bad(String detail) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, detail);
    }

    public record PrivateDeliveryUrl(String mediaAssetId, String url, Instant expiresAt) {}
}
