package com.mentormatching.media.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.cloudinary.Cloudinary;
import com.mentormatching.media.config.MediaProperties;
import com.mentormatching.media.domain.MediaAsset;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CloudinaryService {
    private final MediaProperties config;
    private final Cloudinary cloudinary;

    public CloudinaryService(MediaProperties config, Cloudinary cloudinary) {
        this.config = config;
        this.cloudinary = cloudinary;
    }

    public void assertConfigured() {
        ensureConfigured();
    }

    public Map<String, Object> uploadIntent(MediaAsset asset) {
        ensureConfigured();
        long timestamp = Instant.now().getEpochSecond();
        String deliveryType = deliveryType(asset);
        Map<String, Object> signedParameters = new LinkedHashMap<>();
        signedParameters.put("public_id", asset.getPublicId());
        signedParameters.put("timestamp", timestamp);
        signedParameters.put("type", deliveryType);

        String signature = cloudinary.apiSignRequest(signedParameters, config.apiSecret(), 1);
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("api_key", config.apiKey());
        fields.put("public_id", asset.getPublicId());
        fields.put("timestamp", timestamp);
        fields.put("type", deliveryType);
        fields.put("signature", signature);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("assetId", asset.getId());
        response.put("uploadUrl", "https://api.cloudinary.com/v1_1/" + config.cloudName()
                + "/" + asset.getResourceType() + "/upload");
        response.put("publicId", asset.getPublicId());
        response.put("resourceType", asset.getResourceType());
        response.put("deliveryType", deliveryType);
        response.put("fields", fields);
        response.put("expiresAt", asset.getExpiresAt());
        return response;
    }

    public Map<String, Object> verify(MediaAsset asset, String returnedAssetId) {
        ensureConfigured();
        Map<?, ?> result;
        try {
            result = cloudinary.api().resource(asset.getPublicId(), Map.of(
                    "resource_type", asset.getResourceType(),
                    "type", deliveryType(asset)));
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Cloudinary asset verification failed", exception);
        }

        String actualAssetId = string(result, "asset_id");
        String actualPublicId = string(result, "public_id");
        String actualResourceType = string(result, "resource_type");
        String actualDeliveryType = string(result, "type");
        String format = string(result, "format");
        String etag = string(result, "etag");
        String secureUrl = string(result, "secure_url");
        long size = number(result.get("bytes"));

        if (!actualAssetId.equals(returnedAssetId)
                || !asset.getPublicId().equals(actualPublicId)
                || !asset.getResourceType().equals(actualResourceType)
                || !deliveryType(asset).equals(actualDeliveryType)) {
            throw bad("Cloudinary asset does not match the upload intent");
        }
        if (size != asset.getExpectedSize() || size > config.maxFileSizeBytes()) {
            throw bad("Uploaded file size does not match the upload intent");
        }
        if (format.isBlank() || etag.isBlank() || !formatMatchesMime(format, asset.getMimeType())) {
            throw bad("Uploaded file format does not match its declared MIME type");
        }
        String expectedUrlPrefix = "https://res.cloudinary.com/" + config.cloudName() + "/";
        if (!secureUrl.startsWith(expectedUrlPrefix)) {
            throw bad("Cloudinary returned an unexpected asset URL");
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("assetId", actualAssetId);
        metadata.put("secureUrl", secureUrl);
        metadata.put("size", size);
        metadata.put("width", result.get("width"));
        metadata.put("height", result.get("height"));
        metadata.put("version", result.get("version"));
        metadata.put("format", format);
        metadata.put("etag", etag);
        return metadata;
    }

    public String privateUrl(MediaAsset asset) {
        ensureConfigured();
        long expiresAt = Instant.now().plus(config.privateUrlTtl()).getEpochSecond();
        try {
            return cloudinary.privateDownload(asset.getPublicId(), asset.getFormat(), Map.of(
                    "resource_type", asset.getResourceType(),
                    "type", deliveryType(asset),
                    "expires_at", expiresAt,
                    "secure", true));
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not generate a private Cloudinary URL", exception);
        }
    }

    public void delete(MediaAsset asset) {
        ensureConfigured();
        try {
            cloudinary.api().deleteResources(List.of(asset.getPublicId()), Map.of(
                    "resource_type", asset.getResourceType(),
                    "type", deliveryType(asset),
                    "invalidate", true));
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Cloudinary asset deletion failed", exception);
        }
    }

    private void ensureConfigured() {
        if (config.cloudName().isBlank() || config.apiKey().isBlank() || config.apiSecret().isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Cloudinary is not configured");
        }
    }

    private static String deliveryType(MediaAsset asset) {
        return "PRIVATE".equals(asset.getAccessType()) ? "authenticated" : "upload";
    }

    private static String string(Map<?, ?> result, String key) {
        Object value = result.get(key);
        return value instanceof String text ? text : "";
    }

    private static long number(Object value) {
        return value instanceof Number number ? number.longValue() : -1;
    }

    private static boolean formatMatchesMime(String format, String mimeType) {
        String normalizedFormat = format.toLowerCase(java.util.Locale.ROOT);
        String normalizedMime = mimeType.toLowerCase(java.util.Locale.ROOT);
        return switch (normalizedMime) {
            case "image/jpeg" -> normalizedFormat.equals("jpg") || normalizedFormat.equals("jpeg");
            case "image/png" -> normalizedFormat.equals("png");
            case "image/webp" -> normalizedFormat.equals("webp");
            case "image/gif" -> normalizedFormat.equals("gif");
            case "video/mp4" -> normalizedFormat.equals("mp4");
            case "video/webm" -> normalizedFormat.equals("webm");
            case "application/pdf" -> normalizedFormat.equals("pdf");
            default -> false;
        };
    }

    private static ResponseStatusException bad(String detail) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, detail);
    }
}
