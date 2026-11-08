package com.mentormatching.media;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

@SpringBootTest(properties = {"EUREKA_ENABLED=false", "CLOUDINARY_CLOUD_NAME=test-cloud",
        "CLOUDINARY_API_KEY=test-key", "CLOUDINARY_API_SECRET=test-secret"})
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

    @Test
    void connectsToMmMediaReplicaSetAndInitializesRequiredIndexes() {
        Document hello = mongo.executeCommand(new Document("hello", 1));
        assertThat(hello.getString("setName")).isNotBlank();
        assertThat(mongo.getDb().getName()).isEqualTo("mm_media");

        Set<String> indexNames = mongo.indexOps("media_assets").getIndexInfo().stream()
                .map(index -> index.getName()).collect(Collectors.toSet());
        assertThat(indexNames).contains("owner_status_created", "status_expires_at", "public_id_unique", "purge_at_ttl");
    }
}
