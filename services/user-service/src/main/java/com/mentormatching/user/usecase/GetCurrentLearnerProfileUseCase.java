package com.mentormatching.user.usecase;

import java.util.Objects;
import java.util.UUID;

import com.mentormatching.user.dto.LearnerProfileDetails;
import com.mentormatching.user.repository.LearnerProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đọc hồ sơ học viên, không tự tạo hồ sơ khi chưa tồn tại.
 */
@Service
@PreAuthorize("hasRole('LEANER')")
@Transactional(readOnly = true)
public class GetCurrentLearnerProfileUseCase {

    private final LearnerProfileRepository repository;

    public GetCurrentLearnerProfileUseCase(LearnerProfileRepository repository) {
        this.repository = repository;
    }

    /**
     * Caller cung cấp userId từ danh tính đã xác thực; yêu cầu role LEANER.
     */
    public LearnerProfileDetails execute(UUID userId) {
        Objects.requireNonNull(userId, "userId is required");
        return repository.findByUserId(userId)
                .map(profile -> LearnerProfileDetails.builder()
                        .id(profile.getId())
                        .userId(profile.getUserId())
                        .gender(profile.getGender())
                        .birthYear(profile.getBirthYear())
                        .schoolName(profile.getSchoolName())
                        .gradeId(profile.getGradeId())
                        .learningGoal(profile.getLearningGoal())
                        .createdAt(profile.getCreatedAt())
                        .updatedAt(profile.getUpdatedAt())
                        .build())
                .orElseGet(() -> LearnerProfileDetails.builder().userId(userId).build());
    }
}
