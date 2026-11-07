package com.mentormatching.catalog.repository;

import com.mentormatching.catalog.entity.SubjectGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubjectGradeRepository extends JpaRepository<SubjectGrade, Long> {
    List<SubjectGrade> findBySubjectId(Long subjectId);

    List<SubjectGrade> findByGradeId(Long gradeId);
}
