package com.mentormatching.mentor.repository;

import java.util.Optional;

import com.mentormatching.mentor.entity.MentorVerification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MentorVerificationRepository extends JpaRepository<MentorVerification, Long> {

    Optional<MentorVerification> findByMentorId(Long mentorId);
}
