package com.mentormatching.mentor.usecase;

import java.util.UUID;

import com.mentormatching.mentor.dto.command.UpsertCurrentMentorSubjectCommand;
import com.mentormatching.mentor.dto.response.MentorSubjectDetails;
import com.mentormatching.mentor.entity.MentorSubject;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import com.mentormatching.mentor.repository.MentorSubjectRepository;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tạo hoặc thay thế môn dạy của mentor đang đăng nhập.
 */
@Service
@PreAuthorize("hasRole('MENTOR')")
public class UpsertCurrentMentorSubjectUseCase {

    private final MentorProfileRepository mentorProfileRepository;
    private final MentorSubjectRepository mentorSubjectRepository;
    private final Validator validator;

    public UpsertCurrentMentorSubjectUseCase(MentorProfileRepository mentorProfileRepository,
                                             MentorSubjectRepository mentorSubjectRepository,
                                             Validator validator) {
        this.mentorProfileRepository = mentorProfileRepository;
        this.mentorSubjectRepository = mentorSubjectRepository;
        this.validator = validator;
    }

    /**
     * Caller lấy userId từ JWT sub. Giữ khóa trên profile trong transaction để các lần ghi
     * của cùng mentor không đồng thời thêm hai dòng có cùng subjectGradeId.
     *
     * @param userId  UUID tài khoản mentor đã xác thực
     * @param command môn dạy cần tạo hoặc thay thế
     * @return dữ liệu môn dạy đã lưu
     * @throws ApiException 400 nếu đầu vào sai; 404 nếu thiếu hồ sơ hoặc môn không thuộc mentor;
     *                      409 nếu môn/lớp đã được mentor này khai báo
     */
    @Transactional
    public MentorSubjectDetails execute(UUID userId, UpsertCurrentMentorSubjectCommand command) {
        if (userId == null || command == null || !validator.validate(command).isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid mentor subject.");
        }

        var profile = mentorProfileRepository.findByUserIdForSubjectUpdate(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MENTOR_PROFILE_NOT_FOUND",
                        "Mentor profile not found."));
        var mentorId = profile.getId();

        MentorSubject subject;
        if (command.id() == null) {
            if (mentorSubjectRepository.existsByMentorIdAndSubjectGradeId(mentorId, command.subjectGradeId())) {
                throw duplicateSubject();
            }
            subject = new MentorSubject();
            subject.setMentorId(mentorId);
        } else {
            subject = mentorSubjectRepository.findByIdAndMentorId(command.id(), mentorId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MENTOR_SUBJECT_NOT_FOUND",
                            "Mentor subject not found."));
            if (mentorSubjectRepository.existsByMentorIdAndSubjectGradeIdAndIdNot(mentorId, command.subjectGradeId(), command.id())) {
                throw duplicateSubject();
            }
        }

        subject.setSubjectGradeId(command.subjectGradeId());
        subject.setProficiencyLevel(command.proficiencyLevel());
        subject.setTeachingNote(command.teachingNote());
        subject.setPricePerHour(command.pricePerHour());
        subject.setActive(command.active());

        var saved = mentorSubjectRepository.save(subject);
        return MentorSubjectDetails.builder()
                .id(saved.getId())
                .mentorId(saved.getMentorId())
                .subjectGradeId(saved.getSubjectGradeId())
                .proficiencyLevel(saved.getProficiencyLevel())
                .teachingNote(saved.getTeachingNote())
                .pricePerHour(saved.getPricePerHour())
                .active(saved.getActive())
                .build();
    }

    private ApiException duplicateSubject() {
        return new ApiException(HttpStatus.CONFLICT, "MENTOR_SUBJECT_ALREADY_EXISTS",
                "Mentor subject already exists for this subject grade.");
    }
}
