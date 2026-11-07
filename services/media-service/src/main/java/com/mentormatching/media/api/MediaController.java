package com.mentormatching.media.api;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.mentormatching.media.domain.MediaAsset;
import com.mentormatching.media.service.MediaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/media")
@Validated
public class MediaController {
    private final MediaService service;
    public MediaController(MediaService service){this.service=service;}

    @PostMapping("/upload-intents")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody MediaRequests.CreateIntent request){
        return service.create(subject(jwt),request.purpose(),request.resourceType(),request.originalFilename(),request.mimeType(),request.fileSizeBytes());
    }

    @PostMapping("/{id}/confirm")
    public MediaRequests.AssetResponse confirm(@AuthenticationPrincipal Jwt jwt,@PathVariable String id,@Valid @RequestBody MediaRequests.Confirm request){
        return toResponse(service.confirm(id,subject(jwt),request.assetId()));
    }

    private static String subject(Jwt jwt){
        try{return java.util.UUID.fromString(jwt.getSubject()).toString();}
        catch(Exception e){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"JWT subject must be a UUID");}
    }
    private static MediaRequests.AssetResponse toResponse(MediaAsset a){
        return new MediaRequests.AssetResponse(a.getId(),a.getStatus(),a.getPurpose(),a.getResourceType(),a.getAccessType(),
                a.getPublicId(),a.getAccessType().equals("PUBLIC")?a.getSecureUrl():null,a.getSize(),a.getWidth(),a.getHeight(),a.getCreatedAt().toString());
    }
}
