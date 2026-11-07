package com.mentormatching.mentor.repository;

import java.util.Optional;
import java.util.UUID;

import com.mentormatching.mentor.entity.MentorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MentorProfileRepository extends JpaRepository<MentorProfile, Long> {

    Optional<MentorProfile> findByUserId(UUID userId);
}
