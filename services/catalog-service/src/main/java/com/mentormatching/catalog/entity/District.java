package com.mentormatching.catalog.entity;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "districts")
public class District {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "city_id", nullable = false)
    private Long cityId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "code", length = 50)
    private String code;

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
