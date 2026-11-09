package com.mentormatching.review.repository;

import com.mentormatching.review.entity.ReviewTagOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewTagOptionRepository extends JpaRepository<ReviewTagOption, Long> {

    List<ReviewTagOption> findByActiveTrueOrderByNameAsc();
}
