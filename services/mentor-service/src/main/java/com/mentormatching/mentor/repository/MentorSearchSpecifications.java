package com.mentormatching.mentor.repository;

import java.util.ArrayList;
import java.util.List;

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
        return matching(input, null);
    }

    public static Specification<MentorProfile> matching(SearchMentorsQuery input, List<Long> resolvedSubjectGradeIds) {
        return visible().and(filters(input, resolvedSubjectGradeIds));
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
                    cb.equal(root.get("approvalStatus"), MentorApprovalStatus.APPROVED),
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

    private static Specification<MentorProfile> filters(SearchMentorsQuery input, List<Long> resolvedSubjectGradeIds) {
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
            // City && District
            Long cityId = input.hometownCityId() != null ? input.hometownCityId() : input.cityId();
            if (cityId != null)
                predicates.add(cb.equal(root.get("hometownCityId"), cityId));

            Long districtId = input.currentDistrictId() != null ? input.currentDistrictId() : input.districtId();
            if (districtId != null)
                predicates.add(cb.equal(root.get("currentDistrictId"), districtId));

            // YOE
            if (input.minExperienceYears() != null)
                predicates.add(cb.ge(root.get("experienceYears"), input.minExperienceYears()));
            if (input.maxExperienceYears() != null)
                predicates.add(cb.le(root.get("experienceYears"), input.maxExperienceYears()));

            // Subject / SubjectGrade / Price
            boolean hasSubjectFilter = input.subjectGradeId() != null
                    || (resolvedSubjectGradeIds != null && (input.subjectId() != null || input.gradeId() != null));

            if (hasSubjectFilter || input.minPrice() != null || input.maxPrice() != null) {
                var sub = query.subquery(Long.class);
                var subject = sub.from(MentorSubject.class);
                var checks = new ArrayList<Predicate>();
                checks.add(cb.equal(subject.get("mentorId"), root.get("id")));
                checks.add(cb.isTrue(subject.get("active")));

                if (input.subjectGradeId() != null) {
                    checks.add(cb.equal(subject.get("subjectGradeId"), input.subjectGradeId()));
                } else if (resolvedSubjectGradeIds != null && (input.subjectId() != null || input.gradeId() != null)) {
                    if (resolvedSubjectGradeIds.isEmpty()) {
                        checks.add(cb.disjunction());
                    } else {
                        checks.add(subject.get("subjectGradeId").in(resolvedSubjectGradeIds));
                    }
                }

                if (input.minPrice() != null) checks.add(cb.ge(subject.get("pricePerHour"), input.minPrice()));
                if (input.maxPrice() != null) checks.add(cb.le(subject.get("pricePerHour"), input.maxPrice()));

                sub.select(subject.get("id")).where(checks.toArray(Predicate[]::new));
                predicates.add(cb.exists(sub));
            }

            // Personality
            if (input.personalityIds() != null) {
                for (Long id : input.personalityIds()) {
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
