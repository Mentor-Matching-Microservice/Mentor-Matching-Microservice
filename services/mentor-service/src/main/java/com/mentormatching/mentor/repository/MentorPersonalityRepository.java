package com.mentormatching.mentor.repository;

import java.util.List;

import com.mentormatching.mentor.entity.MentorPersonality;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MentorPersonalityRepository extends JpaRepository<MentorPersonality, Long> {

    List<MentorPersonality> findAllByMentorId(Long mentorId);

    long countByMentorId(Long mentorId);
}
