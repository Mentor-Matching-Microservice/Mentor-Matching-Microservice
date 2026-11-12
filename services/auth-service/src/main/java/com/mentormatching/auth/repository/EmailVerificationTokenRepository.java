package com.mentormatching.auth.repository;

import com.mentormatching.auth.entity.EmailVerificationToken;

import java.time.Instant;
import java.util.UUID;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Truy cập dữ liệu token xác minh email.
 */
@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    /**
     * Tìm token theo hash, không tra cứu bằng token gốc.
     */
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    /** Khoá token khi dùng để hai request đồng thời không cùng dùng được một token. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailVerificationToken> findLockedByTokenHash(String tokenHash);

    /** Token mới nhất của tài khoản (để giới hạn tần suất gửi lại). */
    Optional<EmailVerificationToken> findFirstByAccount_IdOrderByExpiresAtDesc(UUID accountId);

    /** Vô hiệu mọi token chưa dùng của tài khoản (sau khi đã xác minh xong). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update EmailVerificationToken t set t.consumedAt = :now where t.account.id = :accountId and t.consumedAt is null")
    int consumeOutstanding(@Param("accountId") UUID accountId, @Param("now") Instant now);
}
