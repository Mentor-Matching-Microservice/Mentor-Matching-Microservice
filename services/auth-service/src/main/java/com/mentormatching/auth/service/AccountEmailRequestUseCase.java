package com.mentormatching.auth.service;

import java.util.Locale;

import com.mentormatching.auth.dto.request.EmailCommand;
import com.mentormatching.auth.entity.AccountStatus;
import com.mentormatching.auth.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Các yêu cầu gửi email theo địa chỉ (quên mật khẩu, gửi lại link xác minh). Luôn trả về như nhau dù email
 * có tồn tại hay không, để không ai dò được email nào đã đăng ký; giới hạn tần suất theo tài khoản.
 */
@Service
public class AccountEmailRequestUseCase {

    private final AccountRepository accounts;
    private final AccountEmailService emails;

    public AccountEmailRequestUseCase(AccountRepository accounts, AccountEmailService emails) {
        this.accounts = accounts;
        this.emails = emails;
    }

    /** Gửi link đặt lại mật khẩu cho tài khoản đang hoạt động hoặc chờ xác minh; tài khoản bị khoá thì bỏ qua. */
    @Transactional
    public void forgotPassword(EmailCommand command) {
        accounts.findByLoginEmail(normalize(command))
                .filter(account -> account.getStatus() == AccountStatus.ACTIVE
                        || account.getStatus() == AccountStatus.PENDING_VERIFICATION)
                .ifPresent(emails::issuePasswordReset);
    }

    /** Gửi lại link xác minh cho tài khoản còn chờ xác minh. */
    @Transactional
    public void resendVerification(EmailCommand command) {
        accounts.findByLoginEmail(normalize(command))
                .filter(account -> account.getStatus() == AccountStatus.PENDING_VERIFICATION)
                .ifPresent(emails::issueEmailVerification);
    }

    private static String normalize(EmailCommand command) {
        return command.email().trim().toLowerCase(Locale.ROOT);
    }
}
