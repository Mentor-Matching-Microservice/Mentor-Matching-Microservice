package com.mentormatching.media.domain;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("media_assets")
@CompoundIndex(name = "owner_status_created", def = "{'ownerId': 1, 'status': 1, 'createdAt': 1}")
public class MediaAsset {
    @Id private String id;
    private String ownerId;
    private String purpose;
    private String resourceType;
    private String accessType;
    private String status;
    private String publicId;
    private String assetId;
    private String secureUrl;
    private String originalFilename;
    private String mimeType;
    private long expectedSize;
    private long size;
    private Integer width;
    private Integer height;
    private Long version;
    private String format;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant expiresAt;
    private Instant deletedAt;
    protected MediaAsset() {}
    public MediaAsset(String ownerId, String purpose, String resourceType, String accessType, String publicId,
                      String originalFilename, String mimeType, long expectedSize, Instant expiresAt) {
        this.ownerId=ownerId; this.purpose=purpose; this.resourceType=resourceType; this.accessType=accessType;
        this.publicId=publicId; this.originalFilename=originalFilename; this.mimeType=mimeType; this.expectedSize=expectedSize;
        this.status="PENDING"; this.createdAt=Instant.now(); this.updatedAt=this.createdAt; this.expiresAt=expiresAt;
    }
    public void ready(String assetId, String secureUrl, long size, Integer width, Integer height, Long version, String format) {
        this.assetId=assetId; this.secureUrl=secureUrl; this.size=size; this.width=width; this.height=height;
        this.version=version; this.format=format; this.status="READY"; this.updatedAt=Instant.now();
    }
    public String getId(){return id;} public String getOwnerId(){return ownerId;} public String getPurpose(){return purpose;}
    public String getResourceType(){return resourceType;} public String getAccessType(){return accessType;} public String getStatus(){return status;}
    public String getPublicId(){return publicId;} public String getAssetId(){return assetId;} public String getSecureUrl(){return secureUrl;}
    public String getOriginalFilename(){return originalFilename;} public String getMimeType(){return mimeType;} public long getExpectedSize(){return expectedSize;}
    public long getSize(){return size;} public Integer getWidth(){return width;} public Integer getHeight(){return height;}
    public Long getVersion(){return version;} public String getFormat(){return format;} public Instant getCreatedAt(){return createdAt;}
    public Instant getUpdatedAt(){return updatedAt;} public Instant getExpiresAt(){return expiresAt;}
}
