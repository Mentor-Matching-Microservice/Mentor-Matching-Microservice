package com.mentormatching.auth.controller;

import java.util.UUID;

import com.mentormatching.auth.dto.response.AccountStatusSnapshot;
import com.mentormatching.auth.exception.ApiException;
import com.mentormatching.auth.exception.ErrorCode;
import com.mentormatching.auth.repository.AccountRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trạng thái một tài khoản cho service nội bộ ({@code GET /internal/v1/accounts/{accountId}/status}).
 * Dùng cùng cơ chế bảo vệ với {@code /internal/accounts/**} (token nội bộ, xem {@code InternalAccountSecurityConfiguration}).
 * Chỉ trả id, trạng thái và version; không trả email hay thông tin đăng nhập.
 */
@RestController
@ConditionalOnProperty(name = "auth.internal.account-sync.enabled", havingValue = "true")
public class InternalAccountController {

    private final AccountRepository accounts;

    public InternalAccountController(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/internal/v1/accounts/{accountId}/status")
    @Transactional(readOnly = true)
    public AccountStatusSnapshot status(@PathVariable UUID accountId) {
        return accounts.findById(accountId)
                .map(a -> AccountStatusSnapshot.builder().userId(a.getId()).status(a.getStatus()).version(a.getVersion()).build())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, "Account not found."));
    }
}
