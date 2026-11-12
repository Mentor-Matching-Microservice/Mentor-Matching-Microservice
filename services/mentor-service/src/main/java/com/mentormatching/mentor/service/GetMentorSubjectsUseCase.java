package com.mentormatching.mentor.service;

import java.util.List;

import com.mentormatching.mentor.dto.response.MentorSubjectDetails;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import com.mentormatching.mentor.repository.MentorSearchSpecifications;
import com.mentormatching.mentor.repository.MentorSubjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đọc các môn đang bật của mentor công khai. */
@Service
@Transactional(readOnly = true)
public class GetMentorSubjectsUseCase {

    private final MentorProfileRepository mentorProfileRepository;
    private final MentorSubjectRepository mentorSubjectRepository;

    public GetMentorSubjectsUseCase(MentorProfileRepository mentorProfileRepository,
                                    MentorSubjectRepository mentorSubjectRepository) {
        this.mentorProfileRepository = mentorProfileRepository;
        this.mentorSubjectRepository = mentorSubjectRepository;
    }

    public List<MentorSubjectDetails> execute(Long mentorId) {
        if (mentorId == null || mentorId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Mentor ID must be positive.");
        }
        mentorProfileRepository.findOne(MentorSearchSpecifications.visible()
                        .and((root, query, cb) -> cb.equal(root.get("id"), mentorId)))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MENTOR_PROFILE_NOT_FOUND", "Mentor profile not found."));

        return mentorSubjectRepository.findAllByMentorIdAndActiveTrueOrderByIdAsc(mentorId).stream()
                .map(subject -> MentorSubjectDetails.builder()
                        .id(subject.getId())
                        .mentorId(subject.getMentorId())
                        .subjectGradeId(subject.getSubjectGradeId())
                        .proficiencyLevel(subject.getProficiencyLevel())
                        .teachingNote(subject.getTeachingNote())
                        .pricePerHour(subject.getPricePerHour())
                        .active(subject.getActive())
                        .build())
                .toList();
    }
}
