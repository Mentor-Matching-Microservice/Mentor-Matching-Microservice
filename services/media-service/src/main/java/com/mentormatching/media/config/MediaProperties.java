package com.mentormatching.media.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("media.cloudinary")
public record MediaProperties(String cloudName, String apiKey, String apiSecret, String uploadFolder,
                              long maxFileSizeBytes, Duration uploadIntentTtl) {
    public MediaProperties {
        cloudName = cloudName == null ? "" : cloudName;
        apiKey = apiKey == null ? "" : apiKey;
        apiSecret = apiSecret == null ? "" : apiSecret;
        uploadFolder = uploadFolder == null || uploadFolder.isBlank() ? "mentor-matching" : uploadFolder;
        uploadIntentTtl = uploadIntentTtl == null ? Duration.ofMinutes(30) : uploadIntentTtl;
    }
}
