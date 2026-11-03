package com.mentormatching.mentor.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "mentor_profiles")
public class MentorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true, columnDefinition = "BINARY(16)", updatable = false)
    private UUID userId;

    /** Display-name copy owned by user-service; nullable until synchronized. */
    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "avatar_url", nullable = false, columnDefinition = "TEXT")
    private String avatarUrl;

    @Column(name = "avatar_media_id")
    private Long avatarMediaId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "gender", length = 50)
    private MentorGender gender;

    @Column(name = "hometown_city_id")
    private Long hometownCityId;

    @Column(name = "current_district_id")
    private Long currentDistrictId;

    @Column(name = "headline")
    private String headline;

    @Column(name = "introduction", columnDefinition = "TEXT")
    private String introduction;

    @Column(name = "teaching_style", columnDefinition = "TEXT")
    private String teachingStyle;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "current_position")
    private String currentPosition;

    @Column(name = "workplace")
    private String workplace;

    @Column(name = "education")
    private String education;

    @Column(name = "major")
    private String major;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "meeting_type", length = 50)
    private MeetingType meetingType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "approval_status", length = 50)
    private MentorApprovalStatus approvalStatus;

    @Column(name = "approval_note", columnDefinition = "TEXT")
    private String approvalNote;

    @Column(name = "approved_by", columnDefinition = "BINARY(16)")
    private UUID approvedBy;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "approved_at", columnDefinition = "DATETIME(6)")
    private Instant approvedAt;

    @CreationTimestamp
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", columnDefinition = "DATETIME(6)", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @UpdateTimestamp
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "updated_at", columnDefinition = "DATETIME(6)", nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant updatedAt;
}
