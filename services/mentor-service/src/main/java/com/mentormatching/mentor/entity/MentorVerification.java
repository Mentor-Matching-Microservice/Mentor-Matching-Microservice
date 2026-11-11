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
@Table(name = "mentor_verifications")
public class MentorVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "mentor_id", nullable = false, unique = true)
    private Long mentorId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "id_card_number", length = 50)
    private String idCardNumber;

    @Column(name = "id_card_front_url", nullable = false, columnDefinition = "TEXT")
    private String idCardFrontUrl;

    @Column(name = "id_card_front_media_id", length = 64)
    private String idCardFrontMediaId;

    @Column(name = "id_card_back_url", nullable = false, columnDefinition = "TEXT")
    private String idCardBackUrl;

    @Column(name = "id_card_back_media_id", length = 64)
    private String idCardBackMediaId;

    @Column(name = "selfie_with_id_url", columnDefinition = "TEXT")
    private String selfieWithIdUrl;

    @Column(name = "selfie_with_id_media_id", length = 64)
    private String selfieWithIdMediaId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "verification_status", nullable = false, length = 50)
    private MentorVerificationStatus verificationStatus = MentorVerificationStatus.PENDING;

    @Column(name = "verified_by", columnDefinition = "BINARY(16)")
    private UUID verifiedBy;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "verified_at", columnDefinition = "DATETIME(6)")
    private Instant verifiedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

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
