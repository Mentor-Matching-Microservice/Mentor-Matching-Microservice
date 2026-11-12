package com.mentormatching.mentor.repository;

import java.util.Optional;

import com.mentormatching.mentor.entity.MentorVerification;
import com.mentormatching.mentor.entity.MentorVerificationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MentorVerificationRepository extends JpaRepository<MentorVerification, Long> {

    Optional<MentorVerification> findByMentorId(Long mentorId);

    Page<MentorVerification> findByVerificationStatus(MentorVerificationStatus status, Pageable pageable);

    /** Khóa bản ghi đang chờ để các lượt review không ghi đè nhau. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from MentorVerification v where v.id = :id")
    Optional<MentorVerification> findForReviewById(@Param("id") Long id);
}
