package com.mentormatching.mentor.repository;

import java.util.List;

import com.mentormatching.mentor.entity.MentorSubject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MentorSubjectRepository extends JpaRepository<MentorSubject, Long> {

    List<MentorSubject> findAllByMentorId(Long mentorId);
}
