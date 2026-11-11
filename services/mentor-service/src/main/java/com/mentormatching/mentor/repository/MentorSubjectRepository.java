package com.mentormatching.mentor.repository;

import java.util.List;
import java.util.Optional;

import com.mentormatching.mentor.entity.MentorSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MentorSubjectRepository extends JpaRepository<MentorSubject, Long> {

    List<MentorSubject> findAllByMentorId(Long mentorId);

    List<MentorSubject> findAllByMentorIdOrderByIdAsc(Long mentorId);

    long countByMentorIdAndActiveTrue(Long mentorId);

    Optional<MentorSubject> findByIdAndMentorId(Long id, Long mentorId);

    boolean existsByMentorIdAndSubjectGradeIdAndIdNot(Long mentorId, Long subjectGradeId, Long id);

    boolean existsByMentorIdAndSubjectGradeId(Long mentorId, Long subjectGradeId);

    boolean existsBySubjectGradeId(Long subjectGradeId);

    boolean existsBySubjectGradeIdIn(List<Long> subjectGradeIds);

    /** Giá thấp nhất của tất cả môn đang hoạt động cho mỗi mentor trong trang. */
    @Query("""
            select s.mentorId as mentorId, min(s.pricePerHour) as minPrice
            from MentorSubject s
            where s.mentorId in :mentorIds and s.active = true and s.pricePerHour is not null
            group by s.mentorId
            """)
    List<MentorPriceProjection> findMinimumActivePrices(@Param("mentorIds") List<Long> mentorIds);
}
