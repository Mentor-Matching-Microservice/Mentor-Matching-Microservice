package com.mentormatching.user.usecase;

import java.time.Year;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

import com.mentormatching.user.dto.LearnerProfileDetails;
import com.mentormatching.user.dto.UpsertCurrentLearnerProfileCommand;
import com.mentormatching.user.entity.LearnerProfile;
import com.mentormatching.user.exception.ApiException;
import com.mentormatching.user.repository.LearnerProfileRepository;
import com.mentormatching.user.repository.UserProfileRepository;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.http.HttpStatus.*;

/**
 * Tạo hoặc thay thế hồ sơ học viên của người dùng hiện tại.
 */
@Service
@PreAuthorize("hasRole('LEANER')")
public class UpsertCurrentLearnerProfileUseCase {

    private final LearnerProfileRepository learners;
    private final UserProfileRepository users;
    private final Validator validator;

    public UpsertCurrentLearnerProfileUseCase(LearnerProfileRepository learners,
                                              UserProfileRepository users, Validator validator) {
        this.learners = learners;
        this.users = users;
        this.validator = validator;
    }

    /**
     * Caller cung cấp userId từ danh tính đã xác thực, không lấy từ request body.
     * Yêu cầu role LEANER; gradeId chỉ được kiểm tra định dạng, chưa tra cứu catalog.
     */
    @Transactional
    public LearnerProfileDetails execute(UUID userId, UpsertCurrentLearnerProfileCommand command) {
        Objects.requireNonNull(userId, "userId is required");

        if (command == null || !validator.validate(command).isEmpty()) {
            throw new ApiException(BAD_REQUEST, "INVALID_LEARNER_PROFILE", "Invalid learner profile data");
        }
        if (command.birthYear() != null && command.birthYear() > Year.now(ZoneOffset.UTC).getValue()) {
            throw new ApiException(BAD_REQUEST, "INVALID_BIRTH_YEAR", "Birth year must not be in the future");
        }
        if (!users.existsById(userId)) {
            throw new ApiException(NOT_FOUND, "USER_PROFILE_NOT_FOUND", "User profile not found");
        }

        var profile = learners.findByUserId(userId).orElseGet(() -> {
            var created = new LearnerProfile();
            created.setUserId(userId);
            return created;
        });

        profile.setGender(command.gender());
        profile.setBirthYear(command.birthYear());
        profile.setSchoolName(command.schoolName());
        profile.setGradeId(command.gradeId());
        profile.setLearningGoal(command.learningGoal());

        // Flush trước khi map để response có timestamps do Hibernate cập nhật.
        var saved = learners.saveAndFlush(profile);
        
        return LearnerProfileDetails.builder()
                .id(saved.getId())
                .userId(saved.getUserId())
                .gender(saved.getGender())
                .birthYear(saved.getBirthYear())
                .schoolName(saved.getSchoolName())
                .gradeId(saved.getGradeId())
                .learningGoal(saved.getLearningGoal())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }
}
