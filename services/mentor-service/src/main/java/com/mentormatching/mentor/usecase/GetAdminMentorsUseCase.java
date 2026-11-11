package com.mentormatching.mentor.usecase;

import java.util.List;

import com.mentormatching.mentor.dto.query.GetAdminMentorsQuery;
import com.mentormatching.mentor.dto.response.AdminMentorSummaryResponse;
import com.mentormatching.mentor.dto.response.PageResponse;
import com.mentormatching.mentor.entity.MentorProfile;
import com.mentormatching.mentor.entity.MentorVerification;
import com.mentormatching.mentor.entity.MentorVerificationStatus;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import com.mentormatching.mentor.repository.MentorSubjectRepository;
import com.mentormatching.mentor.repository.MentorVerificationRepository;
import jakarta.validation.Validator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('ADMIN')")
@Transactional(readOnly = true)
public class GetAdminMentorsUseCase {

    private final MentorProfileRepository mentorProfileRepository;
    private final MentorVerificationRepository mentorVerificationRepository;
    private final MentorSubjectRepository mentorSubjectRepository;
    private final Validator validator;

    public GetAdminMentorsUseCase(MentorProfileRepository mentorProfileRepository,
                                  MentorVerificationRepository mentorVerificationRepository,
                                  MentorSubjectRepository mentorSubjectRepository,
                                  Validator validator) {
        this.mentorProfileRepository = mentorProfileRepository;
        this.mentorVerificationRepository = mentorVerificationRepository;
        this.mentorSubjectRepository = mentorSubjectRepository;
        this.validator = validator;
    }

    public PageResponse<AdminMentorSummaryResponse> execute(GetAdminMentorsQuery query) {
        if (query == null || !validator.validate(query).isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid mentor query.");
        }

        var pageable = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(Sort.Direction.DESC, "createdAt", "id")
        );

        Specification<MentorProfile> spec = (root, q, cb) -> cb.conjunction();
        if (query.approvalStatus() != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("approvalStatus"), query.approvalStatus()));
        }
        if (query.search() != null && !query.search().isBlank()) {
            String pattern = "%" + query.search().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("headline")), pattern)
            ));
        }

        Page<MentorProfile> page = mentorProfileRepository.findAll(spec, pageable);

        List<AdminMentorSummaryResponse> items = page.getContent().stream().map(p -> {
            var verificationOpt = mentorVerificationRepository.findByMentorId(p.getId());
            MentorVerificationStatus kycStatus = verificationOpt
                    .map(MentorVerification::getVerificationStatus)
                    .orElse(null);
            long activeSubjects = mentorSubjectRepository.countByMentorIdAndActiveTrue(p.getId());

            return AdminMentorSummaryResponse.builder()
                    .id(p.getId())
                    .userId(p.getUserId())
                    .fullName(p.getFullName())
                    .headline(p.getHeadline())
                    .avatarUrl(p.getAvatarUrl())
                    .approvalStatus(p.getApprovalStatus())
                    .accountStatus(p.getAccountStatus())
                    .kycStatus(kycStatus)
                    .activeSubjectsCount(activeSubjects)
                    .createdAt(p.getCreatedAt())
                    .updatedAt(p.getUpdatedAt())
                    .build();
        }).toList();

        return PageResponse.<AdminMentorSummaryResponse>builder()
                .page(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalPages(page.getTotalPages())
                .totalItems(page.getTotalElements())
                .data(items)
                .build();
    }
}
