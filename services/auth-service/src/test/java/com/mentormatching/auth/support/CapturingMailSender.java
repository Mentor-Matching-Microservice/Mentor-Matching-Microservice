package com.mentormatching.auth.support;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.mentormatching.auth.mail.AccountMailSender;

/** Giữ lại email "đã gửi" trong bộ nhớ để test lấy token thô như người dùng mở email. */
public class CapturingMailSender implements AccountMailSender {

    public enum Kind { VERIFY_EMAIL, RESET_PASSWORD }

    public record SentMail(Kind kind, String email, String rawToken, Instant expiresAt) {
    }

    private final List<SentMail> sent = new CopyOnWriteArrayList<>();

    @Override
    public void sendEmailVerification(String email, String rawToken, Instant expiresAt) {
        sent.add(new SentMail(Kind.VERIFY_EMAIL, email, rawToken, expiresAt));
    }

    @Override
    public void sendPasswordReset(String email, String rawToken, Instant expiresAt) {
        sent.add(new SentMail(Kind.RESET_PASSWORD, email, rawToken, expiresAt));
    }

    public List<SentMail> to(String email, Kind kind) {
        return sent.stream().filter(m -> m.email().equals(email) && m.kind() == kind).toList();
    }

    public String latestToken(String email, Kind kind) {
        List<SentMail> mails = to(email, kind);
        if (mails.isEmpty()) {
            throw new AssertionError("no " + kind + " email sent to " + email);
        }
        return mails.getLast().rawToken();
    }

    public void clear() {
        sent.clear();
    }
}
