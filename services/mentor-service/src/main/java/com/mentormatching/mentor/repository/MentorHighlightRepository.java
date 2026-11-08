package com.mentormatching.mentor.repository;

import java.util.List;

import com.mentormatching.mentor.entity.MentorHighlight;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MentorHighlightRepository extends JpaRepository<MentorHighlight, Long> {

    List<MentorHighlight> findAllByMentorId(Long mentorId);
}
