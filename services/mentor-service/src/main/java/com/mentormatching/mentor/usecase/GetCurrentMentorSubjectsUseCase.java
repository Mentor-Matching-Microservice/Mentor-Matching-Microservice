package com.mentormatching.mentor.usecase;

import java.util.List;
import java.util.UUID;

import com.mentormatching.mentor.dto.response.MentorSubjectDetails;
import com.mentormatching.mentor.entity.MentorSubject;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import com.mentormatching.mentor.repository.MentorSubjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đọc toàn bộ môn dạy của mentor đang đăng nhập, gồm cả các môn đã tắt.
 */
@Service
@PreAuthorize("hasRole('MENTOR')")
@Transactional(readOnly = true)
public class GetCurrentMentorSubjectsUseCase {

    private final MentorProfileRepository mentorProfileRepository;
    private final MentorSubjectRepository mentorSubjectRepository;

    public GetCurrentMentorSubjectsUseCase(MentorProfileRepository mentorProfileRepository, MentorSubjectRepository mentorSubjectRepository) {
        this.mentorProfileRepository = mentorProfileRepository;
        this.mentorSubjectRepository = mentorSubjectRepository;
    }

    /**
     * Caller lấy userId từ JWT sub; không nhận mentorId từ request.
     *
     * @param userId UUID tài khoản mentor đã xác thực
     * @return các môn dạy theo ID tăng dần, hoặc danh sách rỗng nếu chưa khai môn
     * @throws ApiException 400 nếu userId null; 404 nếu chưa có hồ sơ mentor
     */
    public List<MentorSubjectDetails> execute(UUID userId) {
        if (userId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "User ID is required.");
        }

        var profile = mentorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MENTOR_PROFILE_NOT_FOUND",
                        "Mentor profile not found."));

        return mentorSubjectRepository.findAllByMentorIdOrderByIdAsc(profile.getId()).stream()
                .map(this::toDetails)
                .toList();
    }

    private MentorSubjectDetails toDetails(MentorSubject subject) {
        return MentorSubjectDetails.builder()
                .id(subject.getId())
                .mentorId(subject.getMentorId())
                .subjectGradeId(subject.getSubjectGradeId())
                .proficiencyLevel(subject.getProficiencyLevel())
                .teachingNote(subject.getTeachingNote())
                .pricePerHour(subject.getPricePerHour())
                .active(subject.getActive())
                .build();
    }
}
