package com.mentormatching.auth.service;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import com.mentormatching.auth.dto.request.ChangeAccountStatusCommand;
import com.mentormatching.auth.dto.response.AccountStatusResult;
import com.mentormatching.auth.entity.Account;
import com.mentormatching.auth.entity.AccountStatus;
import com.mentormatching.auth.exception.ApiException;
import com.mentormatching.auth.exception.ErrorCode;
import com.mentormatching.auth.messaging.AccountEvents;
import com.mentormatching.auth.repository.AccountRepository;
import com.mentormatching.auth.repository.RefreshTokenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin khoá / vô hiệu / mở khoá tài khoản (#64).
 * <ul>
 *   <li>Chỉ đặt được {@code ACTIVE}, {@code LOCKED}, {@code DISABLED}; admin không đổi được chính mình.</li>
 *   <li>Chuyển sang {@code LOCKED}/{@code DISABLED}: <b>thu hồi ngay mọi refresh token</b> (không làm mới được nữa).
 *       Access token đã cấp vẫn còn hiệu lực tới khi hết hạn (mặc định 15 phút).</li>
 *   <li>Mỗi lần đổi thật sự: ghi {@code AccountStatusChanged} (kèm {@code aggregateVersion}) vào outbox trong cùng
 *       transaction. Đặt lại đúng trạng thái hiện tại thì không có thay đổi và không phát sự kiện.</li>
 * </ul>
 */
@Service
public class ChangeAccountStatusUseCase {

    static final Set<AccountStatus> SETTABLE = EnumSet.of(AccountStatus.ACTIVE, AccountStatus.LOCKED, AccountStatus.DISABLED);

    private final AccountRepository accounts;
    private final RefreshTokenRepository refreshTokens;
    private final AccountEvents events;

    public ChangeAccountStatusUseCase(AccountRepository accounts, RefreshTokenRepository refreshTokens,
                                      AccountEvents events) {
        this.accounts = accounts;
        this.refreshTokens = refreshTokens;
        this.events = events;
    }

    @Transactional
    public AccountStatusResult change(UUID adminAccountId, UUID accountId, ChangeAccountStatusCommand command) {
        if (!SETTABLE.contains(command.status())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST,
                    "Status must be ACTIVE, LOCKED or DISABLED.");
        }
        if (accountId.equals(adminAccountId)) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.CONFLICT, "Administrators cannot change their own status.");
        }
        Account account = accounts.findForRoleGrantById(accountId) // khoá dòng: hai admin đổi cùng lúc không đè nhau
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, "Account not found."));
        AccountStatus previous = account.getStatus();
        if (previous == command.status()) {
            return new AccountStatusResult(account.getId(), previous, account.getVersion(), 0);
        }
        account.setStatus(command.status());
        account = accounts.saveAndFlush(account); // tăng version → aggregateVersion của sự kiện
        int revoked = 0;
        if (command.status() != AccountStatus.ACTIVE) {
            revoked = refreshTokens.revokeAllForAccount(account.getId(), Instant.now());
        }
        String reason = command.reason() == null || command.reason().isBlank() ? null : command.reason().strip();
        events.accountStatusChanged(account, previous, account.getVersion(), reason);
        return new AccountStatusResult(account.getId(), account.getStatus(), account.getVersion(), revoked);
    }
}
