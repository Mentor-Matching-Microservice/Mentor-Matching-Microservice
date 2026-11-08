package com.mentormatching.booking.repository;

import com.mentormatching.booking.entity.AvailabilityType;
import com.mentormatching.booking.entity.MentorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MentorAvailabilityRepository extends JpaRepository<MentorAvailability, Long> {

    List<MentorAvailability> findByMentorId(Long mentorId);

    List<MentorAvailability> findByMentorUserId(String mentorUserId);

    List<MentorAvailability> findByMentorIdOrderByAvailabilityTypeAscDayOfWeekAscAvailableDateAscStartTimeAsc(Long mentorId);

    List<MentorAvailability> findByMentorUserIdOrderByAvailabilityTypeAscDayOfWeekAscAvailableDateAscStartTimeAsc(String mentorUserId);

    @Query("""
            SELECT a
            FROM MentorAvailability a
            WHERE a.mentorId = :mentorId
              AND (
                  a.availabilityType = com.mentormatching.booking.entity.AvailabilityType.RECURRING
                  OR (
                      a.availabilityType = com.mentormatching.booking.entity.AvailabilityType.SPECIFIC_DATE
                      AND a.availableDate BETWEEN :from AND :to
                  )
              )
            ORDER BY a.availabilityType ASC,
                     a.dayOfWeek ASC,
                     a.availableDate ASC,
                     a.startTime ASC
            """)
    List<MentorAvailability> findCalendarAvailabilities(
            @Param("mentorId") Long mentorId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
