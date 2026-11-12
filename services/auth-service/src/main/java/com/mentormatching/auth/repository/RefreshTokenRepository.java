package com.mentormatching.auth.repository;

import com.mentormatching.auth.entity.RefreshToken;

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
 * Truy cập dữ liệu refresh token.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Tìm token theo hash, không tra cứu bằng token gốc.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Khóa token trong transaction để ngăn hai lần rotation đồng thời. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findLockedByTokenHash(String tokenHash);

    /** Thu hồi mọi token còn hiệu lực của một họ (phát hiện token cũ bị dùng lại). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now where t.familyId = :familyId and t.revokedAt is null")
    int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now);

    /** Thu hồi mọi token còn hiệu lực của một tài khoản (đổi mật khẩu, khoá tài khoản). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now where t.account.id = :accountId and t.revokedAt is null")
    int revokeAllForAccount(@Param("accountId") UUID accountId, @Param("now") Instant now);
}
