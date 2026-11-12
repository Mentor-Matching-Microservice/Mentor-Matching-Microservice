package com.mentormatching.notification.support;

import java.util.List;
import java.util.Map;

import com.mentormatching.notification.exception.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller CHỈ có trong test, dùng để kiểm tra nền tảng bảo mật và hợp đồng lỗi trước khi
 * notification-service có API nghiệp vụ (#73). Không nằm trong jar phát hành.
 */
@RestController
@RequestMapping("/test-probe")
public class ContractProbeController {

    public static final String INTERNAL_DETAIL = "internal-detail-that-must-not-leak";

    public record ProbeCommand(@NotBlank @Size(max = 20) String title) {}

    @GetMapping("/me")
    Map<String, Object> me(Authentication authentication) {
        List<String> authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        return Map.of("userId", authentication.getName(), "authorities", authorities);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    Map<String, String> adminOnly() {
        return Map.of("status", "ok");
    }

    @GetMapping("/business-error")
    void businessError() {
        throw new ApiException(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "Notification not found.");
    }

    @GetMapping("/unexpected-error")
    void unexpectedError() {
        throw new IllegalStateException(INTERNAL_DETAIL);
    }

    @PostMapping("/validate")
    Map<String, String> validate(@Valid @RequestBody ProbeCommand command) {
        return Map.of("title", command.title());
    }
}
