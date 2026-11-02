package com.mentormatching.mentor.repository;

import java.util.List;

import com.mentormatching.mentor.entity.MentorAchievement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MentorAchievementRepository extends JpaRepository<MentorAchievement, Long> {

    List<MentorAchievement> findAllByMentorId(Long mentorId);
}
