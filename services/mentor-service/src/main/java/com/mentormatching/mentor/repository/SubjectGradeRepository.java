package com.mentormatching.mentor.repository;

import com.mentormatching.mentor.entity.SubjectGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SubjectGradeRepository extends JpaRepository<SubjectGrade, Long> {
    List<SubjectGrade> findBySubjectId(Long subjectId);
    List<SubjectGrade> findByGradeId(Long gradeId);
    Optional<SubjectGrade> findBySubjectIdAndGradeId(Long subjectId, Long gradeId);
    Optional<SubjectGrade> findBySubjectIdAndGradeIdIsNull(Long subjectId);
    boolean existsBySubjectIdAndGradeId(Long subjectId, Long gradeId);
    void deleteBySubjectId(Long subjectId);
    void deleteByGradeId(Long gradeId);
}

