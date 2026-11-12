package com.mentormatching.auth.controller;

import com.mentormatching.auth.dto.request.EmailCommand;
import com.mentormatching.auth.dto.request.OneTimeTokenCommand;
import com.mentormatching.auth.dto.request.ResetPasswordCommand;
import com.mentormatching.auth.dto.response.ApiResponse;
import com.mentormatching.auth.dto.response.VerifyEmailResult;
import com.mentormatching.auth.service.AccountEmailRequestUseCase;
import com.mentormatching.auth.service.ResetPasswordUseCase;
import com.mentormatching.auth.service.VerifyEmailUseCase;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Xác minh email và khôi phục mật khẩu (công khai, không cần đăng nhập).
 * {@code forgot-password} và {@code verify-email/resend} luôn trả 202 để không lộ email nào đã đăng ký.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AccountRecoveryController {

    public static final String VERIFY_EMAIL = "/api/v1/auth/verify-email";
    public static final String RESEND_VERIFICATION = "/api/v1/auth/verify-email/resend";
    public static final String FORGOT_PASSWORD = "/api/v1/auth/forgot-password";
    public static final String RESET_PASSWORD = "/api/v1/auth/reset-password";

    private final VerifyEmailUseCase verifyEmail;
    private final AccountEmailRequestUseCase emailRequests;
    private final ResetPasswordUseCase resetPassword;

    public AccountRecoveryController(VerifyEmailUseCase verifyEmail, AccountEmailRequestUseCase emailRequests,
                                     ResetPasswordUseCase resetPassword) {
        this.verifyEmail = verifyEmail;
        this.emailRequests = emailRequests;
        this.resetPassword = resetPassword;
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<VerifyEmailResult>> verifyEmail(@Valid @RequestBody OneTimeTokenCommand command) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.<VerifyEmailResult>builder().status(200).code("SUCCESS").success(true)
                        .message("Email verified successfully").data(verifyEmail.verify(command)).build());
    }

    @PostMapping("/verify-email/resend")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@Valid @RequestBody EmailCommand command) {
        emailRequests.resendVerification(command);
        return accepted("If the account is waiting for verification, a new verification email has been sent");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody EmailCommand command) {
        emailRequests.forgotPassword(command);
        return accepted("If the email belongs to an account, a password reset email has been sent");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordCommand command) {
        resetPassword.reset(command);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.<Void>builder().status(200).code("SUCCESS").success(true)
                        .message("Password has been reset; please sign in again").build());
    }

    private static ResponseEntity<ApiResponse<Void>> accepted(String message) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).cacheControl(CacheControl.noStore())
                .body(ApiResponse.<Void>builder().status(202).code("ACCEPTED").success(true).message(message).build());
    }
}
