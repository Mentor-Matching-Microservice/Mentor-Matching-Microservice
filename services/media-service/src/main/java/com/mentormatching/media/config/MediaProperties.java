package com.mentormatching.media.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("media.cloudinary")
public record MediaProperties(String cloudName, String apiKey, String apiSecret, String uploadFolder,
                              long maxFileSizeBytes, Duration uploadIntentTtl, Duration privateUrlTtl,
                              long cleanupDelayMs) {
    public MediaProperties {
        cloudName = cloudName == null ? "" : cloudName;
        apiKey = apiKey == null ? "" : apiKey;
        apiSecret = apiSecret == null ? "" : apiSecret;
        uploadFolder = uploadFolder == null || uploadFolder.isBlank() ? "mentor-matching" : uploadFolder;
        uploadIntentTtl = uploadIntentTtl == null ? Duration.ofMinutes(30) : uploadIntentTtl;
        if (privateUrlTtl == null || privateUrlTtl.compareTo(Duration.ofMinutes(5)) < 0
                || privateUrlTtl.compareTo(Duration.ofMinutes(15)) > 0) {
            privateUrlTtl = Duration.ofMinutes(10);
        }
        if (cleanupDelayMs < 1000) cleanupDelayMs = 3600000;
    }
}
