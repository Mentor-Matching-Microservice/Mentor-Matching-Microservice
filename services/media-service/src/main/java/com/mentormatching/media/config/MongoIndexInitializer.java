package com.mentormatching.media.config;

import java.time.Duration;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/** Creates and validates the indexes required by the media asset lifecycle. */
@Component
public class MongoIndexInitializer implements ApplicationRunner {
    private final MongoOperations mongo;

    public MongoIndexInitializer(MongoOperations mongo) {
        this.mongo = mongo;
    }

    @Override
    public void run(ApplicationArguments args) {
        var indexes = mongo.indexOps("media_assets");
        indexes.ensureIndex(new Index().on("ownerId", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC).on("createdAt", Sort.Direction.ASC)
                .named("owner_status_created"));
        indexes.ensureIndex(new Index().on("status", Sort.Direction.ASC)
                .on("expiresAt", Sort.Direction.ASC).named("status_expires_at"));
        indexes.ensureIndex(new Index().on("publicId", Sort.Direction.ASC)
                .unique().named("public_id_unique"));
        indexes.ensureIndex(new Index().on("purgeAt", Sort.Direction.ASC)
                .expire(Duration.ZERO).named("purge_at_ttl"));
    }
}
