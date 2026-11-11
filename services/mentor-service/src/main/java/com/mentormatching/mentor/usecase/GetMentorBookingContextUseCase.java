package com.mentormatching.mentor.usecase;

import com.mentormatching.mentor.dto.response.MentorBookingContextResponse;
import com.mentormatching.mentor.entity.Grade;
import com.mentormatching.mentor.entity.Subject;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.GradeRepository;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import com.mentormatching.mentor.repository.MentorSubjectRepository;
import com.mentormatching.mentor.repository.SubjectGradeRepository;
import com.mentormatching.mentor.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMentorBookingContextUseCase {

    private final MentorProfileRepository mentorProfileRepository;
    private final MentorSubjectRepository mentorSubjectRepository;
    private final SubjectGradeRepository subjectGradeRepository;
    private final SubjectRepository subjectRepository;
    private final GradeRepository gradeRepository;

    public MentorBookingContextResponse execute(Long mentorId, Long mentorSubjectId) {
        if (mentorId == null || mentorId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Mentor ID must be positive.");
        }
        if (mentorSubjectId == null || mentorSubjectId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Mentor subject ID must be positive.");
        }

        var profile = mentorProfileRepository.findById(mentorId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MENTOR_PROFILE_NOT_FOUND", "Mentor profile not found."));

        var subject = mentorSubjectRepository.findByIdAndMentorId(mentorSubjectId, mentorId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MENTOR_SUBJECT_NOT_FOUND", "Mentor subject not found."));

        String subjectName = null;
        String gradeName = null;

        var subjectGradeOpt = subjectGradeRepository.findById(subject.getSubjectGradeId());
        if (subjectGradeOpt.isPresent()) {
            var sg = subjectGradeOpt.get();
            subjectName = subjectRepository.findById(sg.getSubjectId()).map(Subject::getName).orElse(null);
            if (sg.getGradeId() != null) {
                gradeName = gradeRepository.findById(sg.getGradeId()).map(Grade::getName).orElse(null);
            }
        }

        return MentorBookingContextResponse.builder()
                .mentorId(profile.getId())
                .mentorUserId(profile.getUserId() != null ? profile.getUserId().toString() : null)
                .mentorName(profile.getFullName())
                .meetingType(profile.getMeetingType() != null ? profile.getMeetingType().name() : null)
                .mentorSubjectId(subject.getId())
                .subjectName(subjectName)
                .gradeName(gradeName)
                .pricePerHour(subject.getPricePerHour())
                .active(Boolean.TRUE.equals(subject.getActive()))
                .build();
    }
}

