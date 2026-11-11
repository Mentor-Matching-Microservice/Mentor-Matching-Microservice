package com.mentormatching.mentor.repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import com.mentormatching.mentor.entity.AccountStatus;
import com.mentormatching.mentor.entity.MentorProfile;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MentorProfileRepository extends JpaRepository<MentorProfile, Long>, JpaSpecificationExecutor<MentorProfile> {

    Optional<MentorProfile> findByUserId(UUID userId);

    /** Tuần tự hóa các lần thay tập tính cách và điểm nổi bật của cùng mentor. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from MentorProfile p where p.userId = :userId")
    Optional<MentorProfile> findByUserIdForTraitsUpdate(@Param("userId") UUID userId);

    /** Khóa hồ sơ để các lần thêm/sửa môn của cùng mentor không vượt qua kiểm tra trùng đồng thời. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from MentorProfile p where p.userId = :userId")
    Optional<MentorProfile> findByUserIdForSubjectUpdate(@Param("userId") UUID userId);

    /** Đọc hồ sơ theo ID tăng dần để đồng bộ tài khoản theo từng batch. */
    List<MentorProfile> findByIdGreaterThanOrderByIdAsc(Long afterId, Pageable pageable);

    /** Chỉ áp dụng trạng thái auth mới hơn bản sao đang lưu. */
    @Modifying
    @Query("""
            update MentorProfile p
            set p.accountStatus = :status, p.accountStatusVersion = :version
            where p.userId = :userId
              and (p.accountStatusVersion is null or p.accountStatusVersion < :version)
            """)
    int applyAccountStatus(@Param("userId") UUID userId, @Param("status") AccountStatus status,
                           @Param("version") long version);

    @Modifying
    @Query("""
            update MentorProfile p
            set p.fullName = :fullName, p.avatarUrl = coalesce(:avatarUrl, p.avatarUrl), p.avatarMediaId = coalesce(:avatarMediaId, p.avatarMediaId)
            where p.userId = :userId
            """)
    int applyUserProfile(@Param("userId") UUID userId, @Param("fullName") String fullName,
                         @Param("avatarUrl") String avatarUrl, @Param("avatarMediaId") Long avatarMediaId);
}
