package com.mentormatching.media.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.mentormatching.media.config.MediaProperties;
import com.mentormatching.media.domain.MediaAsset;

@Service
public class CloudinaryService {
    private final MediaProperties config;
    private final RestClient http = RestClient.create();
    public CloudinaryService(MediaProperties config) { this.config=config; }

    public Map<String,Object> uploadIntent(MediaAsset asset) {
        ensureConfigured();
        long timestamp=Instant.now().getEpochSecond();
        Map<String,String> signed=new TreeMap<>();
        signed.put("public_id",asset.getPublicId()); signed.put("timestamp",Long.toString(timestamp));
        signed.put("type",asset.getAccessType().equals("PRIVATE")?"authenticated":"upload");
        Map<String,Object> response=new LinkedHashMap<>();
        response.put("assetId",asset.getId());
        response.put("uploadUrl","https://api.cloudinary.com/v1_1/"+config.cloudName()+"/"+asset.getResourceType()+"/upload");
        response.put("publicId",asset.getPublicId()); response.put("resourceType",asset.getResourceType());
        Map<String,String> fields=new LinkedHashMap<>(); fields.put("api_key",config.apiKey()); fields.put("public_id",asset.getPublicId());
        fields.put("timestamp",Long.toString(timestamp)); fields.put("type",signed.get("type"));
        fields.put("signature",uploadSignature(join(signed)+config.apiSecret())); response.put("fields",fields);
        response.put("expiresAt",asset.getExpiresAt());
        return response;
    }

    public void assertConfigured(){ensureConfigured();}

    @SuppressWarnings("unchecked")
    public Map<String,Object> verify(MediaAsset asset, String returnedAssetId) {
        ensureConfigured();
        String path="https://api.cloudinary.com/v1_1/"+config.cloudName()+"/resources/"+asset.getResourceType()+"/"+
                (asset.getAccessType().equals("PRIVATE")?"authenticated":"upload")+"/"+asset.getPublicId();
        Map<String,Object> result=call(HttpMethod.GET,path,null);
        String actualAssetId=String.valueOf(result.get("asset_id"));
        if (!actualAssetId.equals(returnedAssetId) || !asset.getPublicId().equals(result.get("public_id")))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cloudinary asset does not match upload intent");
        if (!asset.getResourceType().equals(result.get("resource_type")))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cloudinary resource type mismatch");
        Object bytes=result.get("bytes"); long size=bytes instanceof Number n?n.longValue():-1;
        if (size<0 || size>asset.getExpectedSize() || size>config.maxFileSizeBytes())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Uploaded file size is invalid");
        Object secureUrl=result.get("secure_url");
        if (!(secureUrl instanceof String url) || !url.startsWith("https://res.cloudinary.com/"+config.cloudName()+"/"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cloudinary URL is invalid");
        if (!(result.get("format") instanceof String format) || format.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cloudinary format is missing");
        Map<String,Object> metadata=new HashMap<>(); metadata.put("assetId",actualAssetId); metadata.put("secureUrl",secureUrl);
        metadata.put("size",size); metadata.put("width",result.get("width")); metadata.put("height",result.get("height"));
        metadata.put("version",result.get("version")); metadata.put("format",result.get("format"));
        return metadata;
    }

    public String privateUrl(MediaAsset asset) {
        ensureConfigured();
        long timestamp=Instant.now().getEpochSecond();
        long expires=Instant.now().plus(config.privateUrlTtl()).getEpochSecond();
        Map<String,String> params=new TreeMap<>();
        params.put("expires_at",Long.toString(expires)); params.put("format",asset.getFormat());
        params.put("public_id",asset.getPublicId()); params.put("timestamp",Long.toString(timestamp));
        params.put("type","authenticated");
        String signature=uploadSignature(join(params)+config.apiSecret());
        String query="timestamp="+timestamp+"&public_id="+encode(asset.getPublicId())+"&format="+encode(asset.getFormat())+
                "&expires_at="+expires+"&type=authenticated&signature="+signature+"&api_key="+encode(config.apiKey());
        return "https://api.cloudinary.com/v1_1/"+config.cloudName()+"/"+asset.getResourceType()+"/download?"+query;
    }

    public void delete(MediaAsset asset) {
        LinkedMultiValueMap<String,String> form=new LinkedMultiValueMap<>();
        String resourceType=asset.getResourceType();
        String delivery=asset.getAccessType().equals("PRIVATE")?"authenticated":"upload";
        if(asset.getAssetId()!=null) {
            form.add("asset_ids[]",asset.getAssetId());
            call(HttpMethod.DELETE,"https://api.cloudinary.com/v1_1/"+config.cloudName()+"/resources",form);
        } else {
            form.add("public_ids[]",asset.getPublicId()); form.add("invalidate","true");
            call(HttpMethod.DELETE,"https://api.cloudinary.com/v1_1/"+config.cloudName()+"/resources/"+resourceType+"/"+delivery,form);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String,Object> call(HttpMethod method,String url,LinkedMultiValueMap<String,String> form) {
        try {
            var request=http.method(method).uri(url).headers(h->h.setBasicAuth(config.apiKey(),config.apiSecret()));
            if (form!=null) request.contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form);
            return request.retrieve().body(Map.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Cloudinary request failed",e);
        }
    }
    private void ensureConfigured() {
        if(config.cloudName().isBlank()||config.apiKey().isBlank()||config.apiSecret().isBlank())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Cloudinary is not configured");
    }
    private static String join(Map<String,String> map) { return map.entrySet().stream().map(e->e.getKey()+"="+e.getValue()).reduce((a,b)->a+"&"+b).orElse(""); }
    private static byte[] digest(String payload) {
        try { return MessageDigest.getInstance("SHA-1").digest(payload.getBytes(StandardCharsets.UTF_8));
        } catch(Exception e){ throw new IllegalStateException("Unable to sign Cloudinary request",e); }
    }
    private static String uploadSignature(String payload){
        StringBuilder out=new StringBuilder(); for(byte b:digest(payload)) out.append(String.format("%02x",b)); return out.toString();
    }
    private static String encode(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
}
