package com.mentormatching.auth.controller;

import java.util.UUID;

import com.mentormatching.auth.dto.request.ChangeAccountStatusCommand;
import com.mentormatching.auth.dto.response.AccountStatusResult;
import com.mentormatching.auth.dto.response.ApiResponse;
import com.mentormatching.auth.service.ChangeAccountStatusUseCase;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quản trị trạng thái tài khoản (thay phần khoá tài khoản của {@code AdminUserController} trong dự án cũ).
 * Chỉ {@code ROLE_ADMIN}, xác thực bằng Bearer access token do chính auth-service ký.
 */
@RestController
@RequestMapping("/api/v1/admin/accounts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAccountController {

    private final ChangeAccountStatusUseCase changeStatus;

    public AdminAccountController(ChangeAccountStatusUseCase changeStatus) {
        this.changeStatus = changeStatus;
    }

    @PatchMapping("/{accountId}/status")
    public ApiResponse<AccountStatusResult> changeStatus(@AuthenticationPrincipal Jwt admin, @PathVariable UUID accountId,
                                                         @Valid @RequestBody ChangeAccountStatusCommand command) {
        AccountStatusResult result = changeStatus.change(UUID.fromString(admin.getSubject()), accountId, command);
        return ApiResponse.<AccountStatusResult>builder().status(200).code("SUCCESS").success(true)
                .message("Account status updated").data(result).build();
    }
}
