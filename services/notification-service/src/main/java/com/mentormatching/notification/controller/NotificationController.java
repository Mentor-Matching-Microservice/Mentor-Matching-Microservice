package com.mentormatching.notification.controller;

import java.util.UUID;

import com.mentormatching.notification.dto.ApiResponse;
import com.mentormatching.notification.dto.NotificationResponse;
import com.mentormatching.notification.dto.PageResponse;
import com.mentormatching.notification.service.NotificationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hộp thư thông báo của người dùng đang đăng nhập (userId = {@code sub} của JWT).
 * Cùng đường dẫn và hình dạng response với dự án cũ để frontend dùng lại các hook hiện có.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(NotificationService.MAX_PAGE_SIZE) int size) {
        return ApiResponse.ok("Get notifications successfully", notifications.list(userId(jwt), page, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok("Get unread count successfully", notifications.unreadCount(userId(jwt)));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markRead(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.ok("Notification marked as read", notifications.markRead(userId(jwt), id));
    }

    /** {@code data} là số thông báo vừa chuyển từ chưa đọc sang đã đọc. */
    @PostMapping("/mark-all-read")
    public ApiResponse<Long> markAllRead(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok("All notifications marked as read", notifications.markAllRead(userId(jwt)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        notifications.delete(userId(jwt), id);
        return ApiResponse.ok("Notification deleted", null);
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject()); // JwtConfiguration đã bảo đảm sub là UUID
    }
}
