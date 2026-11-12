package com.mentormatching.media.api;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import com.mentormatching.media.domain.MediaAsset;
import com.mentormatching.media.service.MediaService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/media")
@Validated
public class MediaInternalController {
    private final MediaService service;

    public MediaInternalController(MediaService service) {
        this.service = service;
    }

    @PostMapping("/verify-and-attach")
    @PreAuthorize("isAnonymous() or hasAnyRole('ADMIN', 'SERVICE')")
    public MediaRequests.UsageResponse verifyAndAttach(@Valid @RequestBody MediaRequests.VerifyAndAttach request) {
        MediaAsset asset = service.verifyAndAttach(request.mediaAssetId(), request.uploadedByUserId(),
                request.purpose(), request.consumer(), request.referenceId());
        String consumer = request.consumer().toLowerCase(Locale.ROOT);
        String referenceId = UUID.fromString(request.referenceId()).toString();
        return new MediaRequests.UsageResponse(asset.getId(), asset.getOwnerId(), asset.getPurpose(),
                asset.getStatus(), consumer, referenceId, consumer + ":" + referenceId);
    }

    @PostMapping("/private-delivery-urls")
    @PreAuthorize("isAnonymous() or hasAnyRole('ADMIN', 'SERVICE')")
    public MediaRequests.PrivateDeliveryUrlsResponse privateDeliveryUrls(
            @Valid @RequestBody MediaRequests.PrivateDeliveryUrls request) {
        List<MediaRequests.PrivateDeliveryUrlResponse> urls = service.privateDeliveryUrls(request.mediaAssetIds())
                .stream()
                .map(url -> new MediaRequests.PrivateDeliveryUrlResponse(
                        url.mediaAssetId(), url.url(), url.expiresAt()))
                .toList();
        return new MediaRequests.PrivateDeliveryUrlsResponse(urls);
    }
}
