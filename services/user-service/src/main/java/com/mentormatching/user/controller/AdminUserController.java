package com.mentormatching.user.controller;

import java.util.UUID;

import com.mentormatching.user.client.AuthAccountClient;
import com.mentormatching.user.dto.ApiResponse;
import com.mentormatching.user.dto.request.ChangeUserStatusCommand;
import com.mentormatching.user.dto.response.AdminUserDetail;
import com.mentormatching.user.dto.response.AdminUserItem;
import com.mentormatching.user.dto.response.PageResponse;
import com.mentormatching.user.dto.response.UserStatusResult;
import com.mentormatching.user.entity.UserType;
import com.mentormatching.user.service.AccountStatusProjectionUseCase;
import com.mentormatching.user.service.AdminUserQueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quản lý người dùng cho Admin (#66). Chỉ {@code ROLE_ADMIN}; LEANER/MENTOR nhận 403 ProblemDetail.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserQueryService queries;
    private final AuthAccountClient authAccounts;
    private final AccountStatusProjectionUseCase statusProjection;

    public AdminUserController(AdminUserQueryService queries, AuthAccountClient authAccounts,
                               AccountStatusProjectionUseCase statusProjection) {
        this.queries = queries;
        this.authAccounts = authAccounts;
        this.statusProjection = statusProjection;
    }

    /** {@code q}: tìm theo họ tên hoặc SĐT (không phân biệt hoa thường); {@code userType}: lọc; {@code page} từ 1. */
    @GetMapping
    public ApiResponse<PageResponse<AdminUserItem>> list(
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(required = false) UserType userType,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(AdminUserQueryService.MAX_PAGE_SIZE) int size) {
        return ok("Get users successfully", queries.search(q, userType, page, size));
    }

    @GetMapping("/{userId}")
    public ApiResponse<AdminUserDetail> detail(@PathVariable UUID userId) {
        return ok("Get user successfully", queries.detail(userId));
    }

    /**
     * Đổi trạng thái tài khoản: chuyển cho auth-service (nơi sở hữu, thu hồi phiên và phát AccountStatusChanged),
     * rồi cập nhật ngay bản sao trạng thái ở user-service.
     */
    @PatchMapping("/{userId}/status")
    public ApiResponse<UserStatusResult> changeStatus(@AuthenticationPrincipal Jwt admin, @PathVariable UUID userId,
                                                      @Valid @RequestBody ChangeUserStatusCommand command) {
        String reason = command.reason() == null || command.reason().isBlank() ? null : command.reason().strip();
        UserStatusResult result = authAccounts.changeStatus(admin.getTokenValue(), userId, command.status(), reason);
        statusProjection.applyFromAdminChange(userId, result.status(), result.aggregateVersion());
        return ok("User status updated", result);
    }

    private static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder().status(200).code("SUCCESS").success(true).message(message).data(data).build();
    }
}
