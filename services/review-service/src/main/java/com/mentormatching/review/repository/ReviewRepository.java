package com.mentormatching.review.repository;

import com.mentormatching.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);

    List<Review> findByMentorId(Long mentorId);

    Page<Review> findByMentorId(Long mentorId, Pageable pageable);

    List<Review> findByStudentUserIdOrderByCreatedAtDesc(String studentUserId);
}
