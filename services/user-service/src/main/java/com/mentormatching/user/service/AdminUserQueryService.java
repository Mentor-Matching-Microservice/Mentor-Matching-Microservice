package com.mentormatching.user.service;

import java.util.Locale;
import java.util.UUID;

import com.mentormatching.user.dto.LearnerProfileDetails;
import com.mentormatching.user.dto.response.AdminUserDetail;
import com.mentormatching.user.dto.response.AdminUserItem;
import com.mentormatching.user.dto.response.PageResponse;
import com.mentormatching.user.entity.UserProfile;
import com.mentormatching.user.entity.UserType;
import com.mentormatching.user.exception.ApiException;
import com.mentormatching.user.repository.LearnerProfileRepository;
import com.mentormatching.user.repository.UserProfileRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Danh sách và chi tiết người dùng cho Admin (#66). Chỉ đọc dữ liệu của chính user-service
 * (trạng thái tài khoản là bản sao từ sự kiện của auth-service, không truy vấn chéo DB).
 */
@Service
@Transactional(readOnly = true)
public class AdminUserQueryService {

    public static final int MAX_PAGE_SIZE = 100;
    static final int MAX_QUERY_LENGTH = 100;

    private final UserProfileRepository profiles;
    private final LearnerProfileRepository learners;

    public AdminUserQueryService(UserProfileRepository profiles, LearnerProfileRepository learners) {
        this.profiles = profiles;
        this.learners = learners;
    }

    /**
     * @param q        tìm trong họ tên hoặc số điện thoại, không phân biệt hoa thường (tuỳ chọn)
     * @param userType lọc theo loại người dùng (tuỳ chọn)
     * @param page     bắt đầu từ 1
     */
    public PageResponse<AdminUserItem> search(String q, UserType userType, int page, int size) {
        Specification<UserProfile> spec = (root, query, cb) -> cb.conjunction();
        if (q != null && !q.isBlank()) {
            String pattern = "%" + escapeLike(q.strip().toLowerCase(Locale.ROOT)) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern, '\\'),
                    cb.like(cb.lower(root.get("phone")), pattern, '\\')));
        }
        if (userType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userType"), userType));
        }
        var pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("userId")));
        return PageResponse.from(profiles.findAll(spec, pageable), AdminUserItem::from);
    }

    public AdminUserDetail detail(UUID userId) {
        UserProfile p = profiles.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_PROFILE_NOT_FOUND", "User profile not found"));
        LearnerProfileDetails learner = learners.findByUserId(userId)
                .map(l -> LearnerProfileDetails.builder().id(l.getId()).userId(l.getUserId()).gender(l.getGender())
                        .birthYear(l.getBirthYear()).schoolName(l.getSchoolName()).gradeId(l.getGradeId())
                        .learningGoal(l.getLearningGoal()).createdAt(l.getCreatedAt()).updatedAt(l.getUpdatedAt()).build())
                .orElse(null);
        return new AdminUserDetail(p.getUserId(), p.getFullName(), p.getEmail(), p.getPhone(), p.getUserType(),
                p.getAccountStatus(), p.getAccountStatusVersion(), p.getCreatedAt(), p.getUpdatedAt(), learner);
    }

    /** Ký tự đại diện của LIKE trong từ khoá được coi là chữ thường, không phải wildcard. */
    static String escapeLike(String raw) {
        String trimmed = raw.length() > MAX_QUERY_LENGTH ? raw.substring(0, MAX_QUERY_LENGTH) : raw;
        return trimmed.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
