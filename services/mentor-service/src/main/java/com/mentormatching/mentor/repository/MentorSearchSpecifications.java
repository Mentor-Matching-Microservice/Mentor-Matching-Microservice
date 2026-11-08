package com.mentormatching.mentor.repository;

import java.util.ArrayList;

import com.mentormatching.mentor.dto.query.SearchMentorsQuery;
import com.mentormatching.mentor.entity.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/**
 * Xây điều kiện Criteria cho tìm kiếm mentor, dùng EXISTS để tránh nhân bản hồ sơ do các bảng con.
 */
public final class MentorSearchSpecifications {

    private MentorSearchSpecifications() {
    }

    /**
     * Kết hợp các bộ lọc bằng AND và chỉ lấy mentor đủ điều kiện hiển thị công khai.
     * Tìm chuỗi con trên tên/headline, escape %, _ và ký tự escape !.
     * Mỗi trait được chọn đều phải tồn tại; môn và giá phải khớp cùng một bản ghi active.
     *
     * @param input query đã được use case validate, không null
     * @return Specification dùng cho cả truy vấn dữ liệu và đếm bản ghi
     */
    public static Specification<MentorProfile> matching(SearchMentorsQuery input) {
        return visible().and(filters(input));
    }

    /** Điều kiện công khai dùng chung cho danh sách và trang chi tiết. */
    public static Specification<MentorProfile> visible() {
        return (root, query, cb) -> {
            var verification = query.subquery(Long.class);
            var v = verification.from(MentorVerification.class);
            verification.select(v.get("id")).where(
                    cb.equal(v.get("mentorId"), root.get("id")),
                    cb.equal(v.get("verificationStatus"), MentorVerificationStatus.VERIFIED));

            var activeSubject = query.subquery(Long.class);
            var s = activeSubject.from(MentorSubject.class);
            activeSubject.select(s.get("id")).where(
                    cb.equal(s.get("mentorId"), root.get("id")),
                    cb.isTrue(s.get("active")));

            return cb.and(
                    cb.equal(root.get("accountStatus"), AccountStatus.ACTIVE),
                    cb.notEqual(cb.trim(root.get("fullName")), ""),
                    cb.notEqual(cb.trim(root.get("avatarUrl")), ""),
                    cb.notEqual(cb.trim(root.get("headline")), ""),
                    cb.notEqual(cb.trim(root.get("introduction")), ""),
                    cb.isNotNull(root.get("meetingType")),
                    cb.exists(verification),
                    cb.exists(activeSubject));
        };
    }

    private static Specification<MentorProfile> filters(SearchMentorsQuery input) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();

            // Search condition
            if (input.search() != null && !input.search().isBlank()) {
                String escaped = input.search().replace("!", "!!").replace("%", "!%").replace("_", "!_");
                predicates.add(cb.or(cb.like(root.get("fullName"), "%" + escaped + "%", '!'),
                        cb.like(root.get("headline"), "%" + escaped + "%", '!')));
            }
            // Gender
            if (input.gender() != null) predicates.add(cb.equal(root.get("gender"), input.gender()));
            // MeetingType
            if (input.meetingType() != null) predicates.add(cb.equal(root.get("meetingType"), input.meetingType()));
            // HomeTown && District
            if (input.hometownCityId() != null)
                predicates.add(cb.equal(root.get("hometownCityId"), input.hometownCityId()));
            if (input.currentDistrictId() != null)
                predicates.add(cb.equal(root.get("currentDistrictId"), input.currentDistrictId()));
            // YOE
            if (input.minExperienceYears() != null)
                predicates.add(cb.ge(root.get("experienceYears"), input.minExperienceYears()));
            if (input.maxExperienceYears() != null)
                predicates.add(cb.le(root.get("experienceYears"), input.maxExperienceYears()));
            // Subject || Min Price || Max Price
            if (input.subjectGradeId() != null || input.minPrice() != null || input.maxPrice() != null) {

                //  SELECT * FROM mentor_profile m
                //  WHERE EXISTS (
                //      SELECT s.id FROM mentor_subject s
                //      WHERE s.mentor_id = m.id
                //      AND s.active = true
                //      AND s.subject_grade_id = 5
                //      AND s.price_per_hour >= minPrice
                //      AND s.price_per_hour <= maxPrice
                //  )
                var sub = query.subquery(Long.class);
                var subject = sub.from(MentorSubject.class); // FROM mentor_subject s
                var checks = new ArrayList<Predicate>();
                checks.add(cb.equal(subject.get("mentorId"), root.get("id")));
                checks.add(cb.isTrue(subject.get("active")));
                if (input.subjectGradeId() != null)
                    checks.add(cb.equal(subject.get("subjectGradeId"), input.subjectGradeId()));
                if (input.minPrice() != null) checks.add(cb.ge(subject.get("pricePerHour"), input.minPrice()));
                if (input.maxPrice() != null) checks.add(cb.le(subject.get("pricePerHour"), input.maxPrice()));
                sub.select(subject.get("id")).where(checks.toArray(Predicate[]::new));
                predicates.add(cb.exists(sub));
            }
            // Personality
            if (input.personalityIds() != null) {
                for (Long id : input.personalityIds()) {
                    // SQL:
                    // SELECT * FROM mentor_profile m
                    //      WHERE EXISTS (
                    //          SELECT 1 FROM mentor_personalities mp
                    //          WHERE mp.mentor_id = m.id AND p.personality_option_id = ?
                    //      )
                    var sub = query.subquery(Long.class);
                    var trait = sub.from(MentorPersonality.class);
                    sub.select(trait.get("id")).where(cb.equal(trait.get("mentorId"), root.get("id")),
                            cb.equal(trait.get("personalityOptionId"), id));
                    predicates.add(cb.exists(sub));
                }
            }
            // Highlight
            if (input.highlightIds() != null) {
                for (Long id : input.highlightIds()) {
                    var sub = query.subquery(Long.class);
                    var trait = sub.from(MentorHighlight.class);
                    sub.select(trait.get("id")).where(cb.equal(trait.get("mentorId"), root.get("id")),
                            cb.equal(trait.get("highlightOptionId"), id));
                    predicates.add(cb.exists(sub));
                }
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
