package com.mentormatching.media.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MediaAssetRepository extends MongoRepository<MediaAsset, String> {
    Optional<MediaAsset> findByIdAndOwnerId(String id, String ownerId);
    List<MediaAsset> findTop100ByStatusAndCreatedAtBefore(String status, Instant before);
}
