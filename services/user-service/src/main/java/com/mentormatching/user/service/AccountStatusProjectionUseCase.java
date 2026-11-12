package com.mentormatching.user.service;

import java.time.Instant;
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
 * Cập nhật bản sao trạng thái tài khoản từ {@code AccountStatusChanged} (#66). Idempotent theo {@code eventId}
 * (inbox cùng transaction) và theo thứ tự: sự kiện có {@code aggregateVersion} không mới hơn bản đang có bị bỏ qua.
 * Nếu sự kiện tới trước {@code AccountRegistered} thì tạo hồ sơ rỗng; email sẽ được bổ sung khi sự kiện kia tới.
 */
@Service
public class AccountStatusProjectionUseCase {

    public enum Outcome { APPLIED, STALE, DUPLICATE }

    private final UserProfileRepository profiles;
    private final InboxEventRepository inbox;
    private final TransactionTemplate transaction;

    public AccountStatusProjectionUseCase(UserProfileRepository profiles, InboxEventRepository inbox,
                                          PlatformTransactionManager transactionManager) {
        this.profiles = profiles;
        this.inbox = inbox;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public Outcome apply(String eventId, String eventType, UUID userId, String status, long aggregateVersion) {
        for (int attempt = 1; ; attempt++) {
            try {
                return transaction.execute(tx -> applyOnce(eventId, eventType, userId, status, aggregateVersion));
            } catch (DataIntegrityViolationException conflict) {
                if (inbox.existsById(eventId)) {
                    return Outcome.DUPLICATE;
                }
                if (attempt >= 2) {
                    throw conflict;
                }
            }
        }
    }

    /** Admin vừa đổi trạng thái qua auth-service: cập nhật ngay bản sao (không chờ sự kiện), cùng quy tắc version. */
    public void applyFromAdminChange(UUID userId, String status, long aggregateVersion) {
        transaction.executeWithoutResult(tx -> profiles.findById(userId).ifPresent(profile -> {
            if (profile.applyAccountStatus(status, aggregateVersion)) {
                profiles.save(profile);
            }
        }));
    }

    private Outcome applyOnce(String eventId, String eventType, UUID userId, String status, long aggregateVersion) {
        if (inbox.existsById(eventId)) {
            return Outcome.DUPLICATE;
        }
        inbox.save(new InboxEvent(eventId, eventType, Instant.now()));
        UserProfile profile = profiles.findById(userId).orElseGet(() -> {
            UserProfile created = new UserProfile();
            created.setUserId(userId);
            return created;
        });
        boolean applied = profile.applyAccountStatus(status, aggregateVersion);
        profiles.saveAndFlush(profile);
        return applied ? Outcome.APPLIED : Outcome.STALE;
    }
}
