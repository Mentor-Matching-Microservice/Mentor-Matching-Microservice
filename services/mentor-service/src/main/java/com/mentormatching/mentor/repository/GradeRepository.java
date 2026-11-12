package com.mentormatching.mentor.repository;

import com.mentormatching.mentor.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GradeRepository extends JpaRepository<Grade, Long> {
    Optional<Grade> findByName(String name);
    boolean existsByName(String name);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}

