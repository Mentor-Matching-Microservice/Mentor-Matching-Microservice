package com.mentormatching.auth.service;

import java.time.Instant;
import java.util.UUID;

import com.mentormatching.auth.entity.Account;
import com.mentormatching.auth.entity.AccountRole;
import com.mentormatching.auth.exception.ApiException;
import com.mentormatching.auth.exception.ErrorCode;
import com.mentormatching.auth.repository.AccountRepository;
import com.mentormatching.auth.repository.AccountRoleRepository;
import com.mentormatching.auth.repository.RoleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gán thêm role {@code MENTOR} cho một tài khoản, idempotent: gọi lại (hoặc nhận trùng sự kiện) không lỗi
 * và không tạo bản ghi trùng. Khoá dòng tài khoản để hai lần gán đồng thời không đụng nhau.
 * Dùng chung cho endpoint nội bộ {@code PUT /internal/accounts/{id}/mentor-role} và consumer {@code MentorApproved}.
 */
@Service
public class GrantMentorRoleUseCase {

    public static final String MENTOR_ROLE = "MENTOR";

    private final AccountRepository accounts;
    private final RoleRepository roles;
    private final AccountRoleRepository accountRoles;

    public GrantMentorRoleUseCase(AccountRepository accounts, RoleRepository roles, AccountRoleRepository accountRoles) {
        this.accounts = accounts;
        this.roles = roles;
        this.accountRoles = accountRoles;
    }

    /** @return true nếu vừa gán role, false nếu tài khoản đã có role MENTOR từ trước */
    @Transactional
    public boolean execute(UUID accountId) {
        Account account = accounts.findForRoleGrantById(accountId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND,
                        "Account not found."));
        var mentor = roles.findByCode(MENTOR_ROLE)
                .orElseThrow(() -> new IllegalStateException("Role MENTOR is not configured"));
        boolean alreadyMentor = accountRoles.findByAccount_Id(account.getId()).stream()
                .anyMatch(assignment -> MENTOR_ROLE.equals(assignment.getRole().getCode()));
        if (alreadyMentor) {
            return false;
        }
        AccountRole assignment = new AccountRole();
        assignment.setAccount(account);
        assignment.setRole(mentor);
        assignment.setAssignedAt(Instant.now());
        accountRoles.save(assignment);
        return true;
    }
}
