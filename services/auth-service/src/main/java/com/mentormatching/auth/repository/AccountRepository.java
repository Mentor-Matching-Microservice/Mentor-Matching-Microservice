package com.mentormatching.auth.repository;

import com.mentormatching.auth.entity.Account;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

/**
 * Truy cập dữ liệu tài khoản.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    /**
     * Tìm tài khoản theo email đăng nhập đã chuẩn hóa.
     */
    Optional<Account> findByLoginEmail(String loginEmail);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findForRoleGrantById(@Param("id") UUID id);
}
