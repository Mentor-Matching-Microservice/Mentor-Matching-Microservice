package com.mentormatching.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
@AutoConfigureMockMvc
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

    @Autowired
    MockMvc mvc;

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
        assertThat(indexNames).contains("owner_status_created", "status_expires_at", "status_created_at",
                "public_id_unique", "purge_at_ttl");
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

    @Test
    void verifyAndAttachChecksOwnerPurposeAndStoresIdempotentUsage() {
        MediaAsset ready = readyAsset("VERIFICATION", "PRIVATE");
        String referenceId = "9faad4e8-6829-4777-907f-c23a0dd975b6";

        MediaAsset attached = media.verifyAndAttach(ready.getId(), userId(), "VERIFICATION",
                "review-service", referenceId);
        MediaAsset repeated = media.verifyAndAttach(ready.getId(), userId(), "VERIFICATION",
                "review-service", referenceId);

        assertThat(attached.getUsageKeys()).containsExactly("review-service:" + referenceId);
        assertThat(repeated.getUsageKeys()).containsExactly("review-service:" + referenceId);
        assertThatThrownBy(() -> media.verifyAndAttach(ready.getId(),
                "d0df3b94-3268-492f-b7b8-119ec8be265c", "VERIFICATION", "review-service", referenceId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> media.verifyAndAttach(ready.getId(), userId(), "AVATAR",
                "review-service", referenceId)).isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void internalVerifyAndAttachEndpointRequiresServiceRole() throws Exception {
        MediaAsset ready = readyAsset("VERIFICATION", "PRIVATE");
        String body = "{\"mediaAssetId\":\"" + ready.getId() + "\",\"uploadedByUserId\":\"" + userId()
                + "\",\"purpose\":\"VERIFICATION\",\"consumer\":\"review-service\","
                + "\"referenceId\":\"9faad4e8-6829-4777-907f-c23a0dd975b6\"}";

        mvc.perform(post("/internal/v1/media/verify-and-attach")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SERVICE")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.usageKey").value("review-service:9faad4e8-6829-4777-907f-c23a0dd975b6"));

        mvc.perform(post("/internal/v1/media/verify-and-attach")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void privateDeliveryUrlsAreSignedShortLivedAndRejectPublicAssets() {
        MediaAsset privateAsset = readyAsset("VERIFICATION", "PRIVATE");
        when(cloudinary.privateUrl(any(MediaAsset.class), any(Instant.class)))
                .thenReturn("https://api.cloudinary.com/v1_1/test-cloud/image/download?expires_at=123");

        var urls = media.privateDeliveryUrls(java.util.List.of(privateAsset.getId()));

        assertThat(urls).hasSize(1);
        assertThat(urls.getFirst().url()).contains("expires_at=");
        assertThat(urls.getFirst().expiresAt()).isAfter(Instant.now()).isBefore(Instant.now().plusSeconds(901));
        MediaAsset publicAsset = readyAsset("AVATAR", "PUBLIC");
        assertThatThrownBy(() -> media.privateDeliveryUrls(java.util.List.of(publicAsset.getId())))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void internalPrivateDeliveryUrlEndpointReturnsOnlyPrivateAssetLinks() throws Exception {
        MediaAsset privateAsset = readyAsset("VERIFICATION", "PRIVATE");
        when(cloudinary.privateUrl(any(MediaAsset.class), any(Instant.class)))
                .thenReturn("https://api.cloudinary.com/v1_1/test-cloud/image/download?expires_at=123");

        mvc.perform(post("/internal/v1/media/private-delivery-urls")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SERVICE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mediaAssetIds\":[\"" + privateAsset.getId() + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urls[0].mediaAssetId").value(privateAsset.getId()))
                .andExpect(jsonPath("$.urls[0].url").value(org.hamcrest.Matchers.containsString("expires_at=")));
    }

    @Test
    void cleanupExpiresOnlyPendingIntentsOlderThanTwentyFourHours() {
        MediaAsset stale = assets.save(pendingAsset("AVATAR", "PUBLIC"));
        MediaAsset recent = assets.save(pendingAsset("AVATAR", "PUBLIC"));
        mongo.updateFirst(org.springframework.data.mongodb.core.query.Query.query(
                        org.springframework.data.mongodb.core.query.Criteria.where("_id").is(stale.getId())),
                new org.springframework.data.mongodb.core.query.Update().set("createdAt", Instant.now().minusSeconds(25 * 60 * 60L)),
                "media_assets");

        media.cleanupExpiredIntents();

        assertThat(assets.findById(stale.getId()).orElseThrow().getStatus()).isEqualTo("EXPIRED");
        assertThat(assets.findById(recent.getId()).orElseThrow().getStatus()).isEqualTo("PENDING");
    }

    private MediaAsset readyAsset(String purpose, String accessType) {
        MediaAsset asset = pendingAsset(purpose, accessType);
        asset.ready("cloud-asset-" + purpose.toLowerCase(),
                "https://res.cloudinary.com/test-cloud/image/upload/asset.png",
                64, 16, 16, 7L, "png", "etag-" + purpose);
        return assets.save(asset);
    }

    private MediaAsset pendingAsset(String purpose, String accessType) {
        return new MediaAsset(userId(), purpose, "image", accessType,
                "mentor-matching/users/" + userId() + "/" + purpose.toLowerCase() + "/" + java.util.UUID.randomUUID(),
                "asset.png", "image/png", 64, Instant.now().plusSeconds(1800));
    }

    private static String userId() {
        return "c9a29bea-7b29-4d4d-aabd-5bf86af200a9";
    }
}
