package com.mentormatching.media.service;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.mentormatching.media.config.MediaProperties;
import com.mentormatching.media.domain.MediaAsset;
import com.mentormatching.media.domain.MediaAssetRepository;

@Service
public class MediaService {
    private final MediaAssetRepository assets; private final CloudinaryService cloud; private final MediaProperties config;
    public MediaService(MediaAssetRepository assets,CloudinaryService cloud,MediaProperties config){this.assets=assets;this.cloud=cloud;this.config=config;}

    public Map<String,Object> create(String owner,String purpose,String resourceType,String filename,String mime,long size) {
        purpose=purpose.toUpperCase(Locale.ROOT); resourceType=resourceType.toLowerCase(Locale.ROOT); mime=mime.toLowerCase(Locale.ROOT);
        if(!SetHolder.PURPOSES.contains(purpose)||!SetHolder.RESOURCES.contains(resourceType)) throw bad("Unsupported media purpose or resource type");
        if(size<1||size>config.maxFileSizeBytes()) throw bad("File size exceeds the allowed limit");
        if(purpose.equals("AVATAR")&&!resourceType.equals("image")) throw bad("Avatar uploads must be images");
        if(resourceType.equals("image")&&!mime.startsWith("image/")) throw bad("Image resource requires image MIME type");
        if(resourceType.equals("video")&&!mime.startsWith("video/")) throw bad("Video resource requires video MIME type");
        cloud.assertConfigured();
        String access=purpose.equals("VERIFICATION")?"PRIVATE":"PUBLIC";
        String publicId=config.uploadFolder()+"/users/"+owner+"/"+purpose.toLowerCase(Locale.ROOT)+"/"+UUID.randomUUID().toString().replace("-","");
        MediaAsset asset=assets.save(new MediaAsset(owner,purpose,resourceType,access,publicId,filename,mime,size,Instant.now().plus(config.uploadIntentTtl())));
        return cloud.uploadIntent(asset);
    }
    public MediaAsset confirm(String id,String owner,String assetId) {
        MediaAsset a=owned(id,owner);
        if(a.getStatus().equals("READY")) { if(!a.getAssetId().equals(assetId)) throw bad("Confirmed asset id differs"); return a; }
        if(!a.getStatus().equals("PENDING")||a.getExpiresAt().isBefore(Instant.now())) throw bad("Upload intent is expired or no longer confirmable");
        Map<String,Object> m=cloud.verify(a,assetId);
        a.ready((String)m.get("assetId"),(String)m.get("secureUrl"),((Number)m.get("size")).longValue(),number(m.get("width")),number(m.get("height")),longNumber(m.get("version")),(String)m.get("format"));
        return assets.save(a);
    }
    public String signedUrl(String id,String owner) {
        MediaAsset a=owned(id,owner); if(!a.getStatus().equals("READY")) throw bad("Media asset is not ready");
        if(!a.getAccessType().equals("PRIVATE")) return a.getSecureUrl();
        return cloud.privateUrl(a);
    }
    public String internalSignedUrl(String id) {
        MediaAsset a=assets.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Media asset not found"));
        if(!a.getStatus().equals("READY")) throw bad("Media asset is not ready");
        return a.getAccessType().equals("PRIVATE")?cloud.privateUrl(a):a.getSecureUrl();
    }
    public void delete(String id,String owner) {
        MediaAsset a=owned(id,owner); if(a.getStatus().equals("DELETED")) return;
        cloud.delete(a); a.delete("DELETED"); assets.save(a);
    }
    private MediaAsset owned(String id,String owner){return assets.findByIdAndOwnerId(id,owner).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Media asset not found"));}
    @Scheduled(fixedDelayString="${media.cloudinary.cleanup-delay-ms:3600000}")
    public void cleanupExpiredIntents(){ for(MediaAsset a:assets.findTop100ByStatusAndExpiresAtBefore("PENDING",Instant.now())) { try { cloud.delete(a); a.delete("EXPIRED"); assets.save(a); } catch(RuntimeException ignored) { /* retry on next cleanup interval */ } } }
    private static Integer number(Object n){return n instanceof Number v?v.intValue():null;}
    private static Long longNumber(Object n){return n instanceof Number v?v.longValue():null;}
    private static ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    private static final class SetHolder { static final java.util.Set<String> PURPOSES=java.util.Set.of("AVATAR","ACHIEVEMENT","REVIEW_ATTACHMENT","VERIFICATION"); static final java.util.Set<String> RESOURCES=java.util.Set.of("image","video","raw"); }
}
