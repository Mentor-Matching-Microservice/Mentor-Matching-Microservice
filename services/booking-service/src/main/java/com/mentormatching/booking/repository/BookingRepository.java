package com.mentormatching.booking.repository;

import com.mentormatching.booking.entity.Booking;
import com.mentormatching.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    List<Booking> findByStudentUserId(String studentUserId);

    List<Booking> findByMentorId(Long mentorId);

    List<Booking> findByMentorUserId(String mentorUserId);

    List<Booking> findByStatus(BookingStatus status);

    @Query("""
            SELECT b
            FROM Booking b
            WHERE b.mentorId = :mentorId
              AND b.bookingDate BETWEEN :from AND :to
              AND (
                  b.status = com.mentormatching.booking.entity.BookingStatus.CONFIRMED
                  OR (
                      b.status = com.mentormatching.booking.entity.BookingStatus.PENDING
                      AND (b.holdExpiresAt IS NULL OR b.holdExpiresAt > CURRENT_TIMESTAMP)
                  )
              )
            ORDER BY b.bookingDate ASC, b.startTime ASC
            """)
    List<Booking> findScheduleBlockingBookings(
            @Param("mentorId") Long mentorId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("statuses") List<BookingStatus> statuses);

    @Query("""
            SELECT COUNT(b) > 0
            FROM Booking b
            WHERE b.mentorId = :mentorId
              AND b.bookingDate = :bookingDate
              AND (
                  b.status = com.mentormatching.booking.entity.BookingStatus.CONFIRMED
                  OR (
                      b.status = com.mentormatching.booking.entity.BookingStatus.PENDING
                      AND (b.holdExpiresAt IS NULL OR b.holdExpiresAt > CURRENT_TIMESTAMP)
                  )
              )
              AND b.startTime < :endTime
              AND b.endTime > :startTime
            """)
    boolean existsOverlappingActiveBooking(
            @Param("mentorId") Long mentorId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    @Query("""
            SELECT b
            FROM Booking b
            WHERE b.status = com.mentormatching.booking.entity.BookingStatus.PENDING
              AND b.holdExpiresAt IS NOT NULL
              AND b.holdExpiresAt <= :now
            """)
    List<Booking> findOverduePendingBookings(@Param("now") LocalDateTime now);
}
