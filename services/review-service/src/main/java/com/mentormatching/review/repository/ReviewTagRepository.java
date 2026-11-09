package com.mentormatching.review.repository;

import com.mentormatching.review.entity.ReviewTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewTagRepository extends JpaRepository<ReviewTag, Long> {

    List<ReviewTag> findByReviewId(Long reviewId);

    List<ReviewTag> findByReviewIdIn(List<Long> reviewIds);

    void deleteByReviewId(Long reviewId);
}
