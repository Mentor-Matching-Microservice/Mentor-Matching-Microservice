package com.mentormatching.mentor.entity;

import java.time.LocalDate;
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
@Table(name = "mentor_achievements")
public class MentorAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "mentor_id", nullable = false)
    private Long mentorId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "achievement_type", length = 50)
    private AchievementType achievementType;

    @Column(name = "issuer")
    private String issuer;

    @Column(name = "achieved_at")
    private LocalDate achievedAt;

    @Column(name = "proof_url", columnDefinition = "TEXT")
    private String proofUrl;

    @Column(name = "proof_media_id")
    private Long proofMediaId;

    @Column(name = "is_verified")
    private Boolean verified = false;

    @Column(name = "verified_by", columnDefinition = "BINARY(16)")
    private UUID verifiedBy;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "verified_at", columnDefinition = "DATETIME(6)")
    private Instant verifiedAt;

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
