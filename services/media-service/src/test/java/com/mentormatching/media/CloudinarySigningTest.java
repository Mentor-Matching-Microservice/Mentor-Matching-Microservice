package com.mentormatching.media;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import com.cloudinary.Cloudinary;
import com.mentormatching.media.config.MediaProperties;
import com.mentormatching.media.domain.MediaAsset;
import com.mentormatching.media.service.CloudinaryService;
import org.junit.jupiter.api.Test;

class CloudinarySigningTest {
    private final MediaProperties properties = new MediaProperties("test-cloud", "test-key", "test-secret",
            "mentor-matching", 5 * 1024 * 1024L, Duration.ofMinutes(30), Duration.ofMinutes(10), 3_600_000);
    private final CloudinaryService cloudinary = new CloudinaryService(properties,
            new Cloudinary(Map.of("cloud_name", properties.cloudName(), "api_key", properties.apiKey(),
                    "api_secret", properties.apiSecret(), "secure", true)));

    @Test
    void signedUploadIntentUsesSdkSignatureAndCorrectDeliveryType() {
        MediaAsset verification = new MediaAsset("owner", "VERIFICATION", "raw", "PRIVATE",
                "mentor-matching/users/owner/verification/abc", "proof.pdf", "application/pdf", 42,
                Instant.now().plus(Duration.ofMinutes(30)));

        Map<String, Object> intent = cloudinary.uploadIntent(verification);
        Map<?, ?> fields = (Map<?, ?>) intent.get("fields");

        assertThat(intent.get("uploadUrl")).isEqualTo("https://api.cloudinary.com/v1_1/test-cloud/raw/upload");
        assertThat(intent.get("deliveryType")).isEqualTo("authenticated");
        assertThat(fields.get("type")).isEqualTo("authenticated");
        assertThat(fields.get("signature")).isInstanceOf(String.class).asString().hasSize(40);
        assertThat(fields.toString()).doesNotContain("test-secret");
    }
}
