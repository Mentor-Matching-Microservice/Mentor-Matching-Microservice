package com.mentormatching.booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_user_id", nullable = false, length = 64)
    private String studentUserId;

    @Column(name = "student_name")
    private String studentName;

    @Column(name = "mentor_id", nullable = false)
    private Long mentorId;

    @Column(name = "mentor_user_id", length = 64)
    private String mentorUserId;

    @Column(name = "mentor_name")
    private String mentorName;

    @Column(name = "mentor_subject_id", nullable = false)
    private Long mentorSubjectId;

    @Column(name = "subject_name")
    private String subjectName;

    @Column(name = "grade_name")
    private String gradeName;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "price_per_hour", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerHour;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "meeting_type", length = 50)
    private MeetingType meetingType;

    @Column(name = "meeting_link", columnDefinition = "NVARCHAR(MAX)")
    private String meetingLink;

    @Column(name = "meeting_address", columnDefinition = "NVARCHAR(MAX)")
    private String meetingAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private BookingStatus status;

    @Column(name = "note", columnDefinition = "NVARCHAR(MAX)")
    private String note;

    @Column(name = "cancelled_by", length = 64)
    private String cancelledBy;

    @Column(name = "cancel_reason", columnDefinition = "NVARCHAR(MAX)")
    private String cancelReason;

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = BookingStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

