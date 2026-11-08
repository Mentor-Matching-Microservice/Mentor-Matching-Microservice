package com.mentormatching.mentor.service;

import java.util.UUID;

import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import com.mentormatching.mentor.repository.MentorSubjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tắt một môn dạy của mentor hiện tại mà không xóa bản ghi. */
@Service
@PreAuthorize("hasRole('MENTOR')")
public class DeleteCurrentMentorSubjectUseCase {

    private final MentorProfileRepository profiles;
    private final MentorSubjectRepository subjects;

    public DeleteCurrentMentorSubjectUseCase(MentorProfileRepository profiles, MentorSubjectRepository subjects) {
        this.profiles = profiles;
        this.subjects = subjects;
    }

    /**
     * Caller lấy userId từ JWT sub. Gọi lại với môn đã tắt vẫn thành công.
     * Mentor có thể bật lại môn bằng use case upsert với cùng ID.
     *
     * @param userId UUID tài khoản mentor đã xác thực
     * @param mentorSubjectId ID môn dạy cần tắt
     * @throws ApiException 400 nếu đầu vào sai; 404 nếu thiếu hồ sơ hoặc môn không thuộc mentor
     */
    @Transactional
    public void execute(UUID userId, Long mentorSubjectId) {
        if (userId == null || mentorSubjectId == null || mentorSubjectId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid mentor subject ID.");
        }

        var profile = profiles.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MENTOR_PROFILE_NOT_FOUND",
                        "Mentor profile not found."));
        var subject = subjects.findByIdAndMentorId(mentorSubjectId, profile.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MENTOR_SUBJECT_NOT_FOUND",
                        "Mentor subject not found."));

        if (!Boolean.FALSE.equals(subject.getActive())) {
            subject.setActive(false);
            subjects.save(subject);
        }
    }
}
