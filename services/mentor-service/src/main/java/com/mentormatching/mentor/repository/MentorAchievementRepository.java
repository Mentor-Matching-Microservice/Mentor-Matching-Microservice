package com.mentormatching.mentor.repository;

import java.util.List;
import java.util.Optional;

import com.mentormatching.mentor.entity.MentorAchievement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MentorAchievementRepository extends JpaRepository<MentorAchievement, Long> {

    List<MentorAchievement> findAllByMentorId(Long mentorId);

    List<MentorAchievement> findAllByMentorIdOrderByIdAsc(Long mentorId);

    long countByMentorId(Long mentorId);

    Optional<MentorAchievement> findByIdAndMentorId(Long id, Long mentorId);
}
