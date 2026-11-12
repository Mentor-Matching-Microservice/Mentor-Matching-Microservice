package com.mentormatching.auth.mail;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Gửi email thật qua SMTP ({@code spring.mail.host}, {@code spring.mail.username}, ... cấu hình qua biến môi trường).
 * Nội dung tiếng Việt, chỉ chứa link một lần; không chứa mật khẩu hay thông tin khác.
 */
public class SmtpAccountMailSender implements AccountMailSender {

    private static final DateTimeFormatter EXPIRY = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final JavaMailSender mailSender;
    private final AccountMailProperties properties;

    public SmtpAccountMailSender(JavaMailSender mailSender, AccountMailProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void sendEmailVerification(String email, String rawToken, Instant expiresAt) {
        send(email, "Xác minh email tài khoản Mentor Matching",
                "Chào bạn,\n\nVui lòng mở link sau để xác minh email và kích hoạt tài khoản:\n"
                        + properties.verifyLink(rawToken)
                        + "\n\nLink có hiệu lực đến " + EXPIRY.format(expiresAt) + " (giờ Việt Nam) và chỉ dùng được một lần."
                        + "\nNếu bạn không đăng ký tài khoản, hãy bỏ qua email này.");
    }

    @Override
    public void sendPasswordReset(String email, String rawToken, Instant expiresAt) {
        send(email, "Đặt lại mật khẩu Mentor Matching",
                "Chào bạn,\n\nChúng tôi nhận được yêu cầu đặt lại mật khẩu. Mở link sau để đặt mật khẩu mới:\n"
                        + properties.resetLink(rawToken)
                        + "\n\nLink có hiệu lực đến " + EXPIRY.format(expiresAt) + " (giờ Việt Nam) và chỉ dùng được một lần."
                        + "\nNếu bạn không yêu cầu, hãy bỏ qua email này; mật khẩu hiện tại vẫn giữ nguyên.");
    }

    private void send(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
