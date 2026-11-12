package com.mentormatching.user.service;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.mentormatching.user.entity.InboxEvent;
import com.mentormatching.user.entity.UserProfile;
import com.mentormatching.user.repository.InboxEventRepository;
import com.mentormatching.user.repository.UserProfileRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Tạo hồ sơ người dùng từ {@code AccountRegistered} (#65), idempotent:
 * <ul>
 *   <li>Bản ghi {@code inbox_events} ({@code eventId}) và việc tạo hồ sơ nằm trong cùng transaction:
 *       nhận lặp sự kiện không tạo thêm gì.</li>
 *   <li>Người dùng gọi {@code PUT /api/v1/users/me} trước khi sự kiện tới: hồ sơ đã có, chỉ bổ sung email,
 *       không ghi đè tên/SĐT người dùng đã nhập.</li>
 *   <li>Hai bên (sự kiện và PUT /me) cùng tạo một lúc: bên chậm hơn gặp trùng khoá chính và được thử lại một lần,
 *       lần sau thấy hồ sơ đã có.</li>
 * </ul>
 */
@Service
public class ProvisionUserProfileUseCase {

    public enum Outcome { CREATED, UPDATED, DUPLICATE }

    private final UserProfileRepository profiles;
    private final InboxEventRepository inbox;
    private final TransactionTemplate transaction;

    public ProvisionUserProfileUseCase(UserProfileRepository profiles, InboxEventRepository inbox,
                                       PlatformTransactionManager transactionManager) {
        this.profiles = profiles;
        this.inbox = inbox;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public Outcome provision(String eventId, String eventType, UUID userId, String email) {
        return provision(eventId, eventType, userId, email, null);
    }

    /** @param status trạng thái tài khoản lúc đăng ký (thường PENDING_VERIFICATION), áp dụng với version 0 */
    public Outcome provision(String eventId, String eventType, UUID userId, String email, String status) {
        String normalizedEmail = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        for (int attempt = 1; ; attempt++) {
            try {
                return transaction.execute(tx -> apply(eventId, eventType, userId, normalizedEmail, status));
            } catch (DataIntegrityViolationException conflict) {
                if (inbox.existsById(eventId)) {
                    return Outcome.DUPLICATE; // một lần giao song song của cùng sự kiện đã commit trước
                }
                if (attempt >= 2) {
                    throw conflict;
                }
                // PUT /me vừa tạo hồ sơ cùng lúc: thử lại, lần này hồ sơ đã tồn tại.
            }
        }
    }

    private Outcome apply(String eventId, String eventType, UUID userId, String email, String status) {
        if (inbox.existsById(eventId)) {
            return Outcome.DUPLICATE;
        }
        inbox.save(new InboxEvent(eventId, eventType, Instant.now()));
        Outcome outcome;
        UserProfile profile = profiles.findById(userId).orElse(null);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setEmail(email);
            outcome = Outcome.CREATED;
        } else {
            if (email != null && !email.equals(profile.getEmail())) {
                profile.setEmail(email);
            }
            outcome = Outcome.UPDATED;
        }
        if (status != null) {
            profile.applyAccountStatus(status, 0);
        }
        profiles.saveAndFlush(profile);
        return outcome;
    }
}
