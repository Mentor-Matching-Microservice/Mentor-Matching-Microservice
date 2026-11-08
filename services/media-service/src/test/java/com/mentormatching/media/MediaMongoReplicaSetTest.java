package com.mentormatching.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import com.mentormatching.media.domain.MediaAsset;
import com.mentormatching.media.domain.MediaAssetRepository;
import com.mentormatching.media.service.CloudinaryService;
import com.mentormatching.media.service.MediaService;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

@SpringBootTest(properties = {"EUREKA_ENABLED=false", "CLOUDINARY_CLOUD_NAME=test-cloud",
        "CLOUDINARY_API_KEY=test-key", "CLOUDINARY_API_SECRET=test-secret",
        "MEDIA_MAX_FILE_SIZE_BYTES=5242880"})
@Testcontainers
class MediaMongoReplicaSetTest {
    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0").withReplicaSet();

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.mongodb.uri", () -> MONGO.getReplicaSetUrl("mm_media"));
    }

    @Autowired
    MongoTemplate mongo;

    @Autowired
    MediaService media;

    @Autowired
    MediaAssetRepository assets;

    @MockitoBean
    CloudinaryService cloudinary;

    @BeforeEach
    void clearAssets() {
        assets.deleteAll();
    }

    @Test
    void connectsToMmMediaReplicaSetAndInitializesRequiredIndexes() {
        Document hello = mongo.executeCommand(new Document("hello", 1));
        assertThat(hello.getString("setName")).isNotBlank();
        assertThat(mongo.getDb().getName()).isEqualTo("mm_media");

        Set<String> indexNames = mongo.indexOps("media_assets").getIndexInfo().stream()
                .map(index -> index.getName()).collect(Collectors.toSet());
        assertThat(indexNames).contains("owner_status_created", "status_expires_at", "public_id_unique", "purge_at_ttl");
    }

    @Test
    void uploadIntentsArePendingAndAccessTypeComesFromPurpose() {
        when(cloudinary.uploadIntent(any(MediaAsset.class))).thenAnswer(invocation -> Map.of(
                "assetId", invocation.<MediaAsset>getArgument(0).getId(), "signature", "signed"));

        media.create(userId(), "AVATAR", "image", "avatar.png", "image/png", 64);
        media.create(userId(), "VERIFICATION", "raw", "proof.pdf", "application/pdf", 128);

        Map<String, String> accessByPurpose = new LinkedHashMap<>();
        for (MediaAsset asset : assets.findAll()) {
            assertThat(asset.getStatus()).isEqualTo("PENDING");
            accessByPurpose.put(asset.getPurpose(), asset.getAccessType());
        }
        assertThat(accessByPurpose).containsEntry("AVATAR", "PUBLIC")
                .containsEntry("VERIFICATION", "PRIVATE");
    }

    @Test
    void intentRejectsOversizedAndMismatchedMimeBeforeSaving() {
        assertThatThrownBy(() -> media.create(userId(), "AVATAR", "image", "large.png", "image/png",
                5 * 1024 * 1024L + 1)).isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> media.create(userId(), "AVATAR", "image", "forged.png", "image/svg+xml", 20))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(assets.count()).isZero();
    }

    @Test
    void confirmationPersistsMetadataReturnedByCloudinaryAdminApi() {
        when(cloudinary.uploadIntent(any(MediaAsset.class))).thenReturn(Map.of("signature", "signed"));
        media.create(userId(), "AVATAR", "image", "avatar.png", "image/png", 64);
        MediaAsset pending = assets.findAll().getFirst();
        when(cloudinary.verify(any(MediaAsset.class), org.mockito.ArgumentMatchers.anyString())).thenReturn(Map.of(
                "assetId", "cloud-asset-1", "secureUrl", "https://res.cloudinary.com/test-cloud/image/upload/avatar.png",
                "size", 64L, "width", 16, "height", 16, "version", 7L, "format", "png", "etag", "etag-1"));

        MediaAsset ready = media.confirm(pending.getId(), userId(), "cloud-asset-1");

        assertThat(ready.getStatus()).isEqualTo("READY");
        assertThat(ready.getAssetId()).isEqualTo("cloud-asset-1");
        assertThat(ready.getEtag()).isEqualTo("etag-1");
        assertThat(ready.getSize()).isEqualTo(64);
        assertThat(ready.getUpdatedAt()).isAfterOrEqualTo(Instant.now().minusSeconds(2));
    }

    @Test
    void confirmationCannotBeClaimedByAnotherUser() {
        when(cloudinary.uploadIntent(any(MediaAsset.class))).thenReturn(Map.of("signature", "signed"));
        media.create(userId(), "AVATAR", "image", "avatar.png", "image/png", 64);
        MediaAsset pending = assets.findAll().getFirst();

        assertThatThrownBy(() -> media.confirm(pending.getId(), "d0df3b94-3268-492f-b7b8-119ec8be265c", "cloud-asset-1"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.NOT_FOUND);
    }

    private static String userId() {
        return "c9a29bea-7b29-4d4d-aabd-5bf86af200a9";
    }
}
