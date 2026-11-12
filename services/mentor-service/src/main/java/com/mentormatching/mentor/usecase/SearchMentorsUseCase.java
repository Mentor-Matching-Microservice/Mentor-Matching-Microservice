package com.mentormatching.mentor.usecase;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.mentormatching.mentor.dto.query.SearchMentorsQuery;
import com.mentormatching.mentor.dto.response.*;
import com.mentormatching.mentor.entity.MentorProfile;
import com.mentormatching.mentor.entity.SubjectGrade;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.*;
import jakarta.validation.Validator;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tìm mentor đã xác minh KYC, tài khoản ACTIVE và hồ sơ đủ thông tin công khai.
 * Lọc và phân trang tại DB mentor; lấy giá tối thiểu theo batch, không gọi service khác.
 * Transaction read-only có hiệu lực khi được gọi qua Spring proxy.
 */
@Service
@Transactional(readOnly = true)
public class SearchMentorsUseCase {

    private final MentorProfileRepository mentorProfileRepository;
    private final MentorSubjectRepository mentorSubjectRepository;
    private final SubjectGradeRepository subjectGradeRepository;
    private final Validator validator;

    public SearchMentorsUseCase(MentorProfileRepository mentorProfileRepository,
                                MentorSubjectRepository mentorSubjectRepository,
                                SubjectGradeRepository subjectGradeRepository,
                                Validator validator) {
        this.mentorProfileRepository = mentorProfileRepository;
        this.mentorSubjectRepository = mentorSubjectRepository;
        this.subjectGradeRepository = subjectGradeRepository;
        this.validator = validator;
    }

    public PageResponse<MentorSearchItemResponse> execute(SearchMentorsQuery query) {
        if (query == null || !validator.validate(query).isEmpty()) {
            throw invalid();
        }

        if (query.minPrice() != null && query.maxPrice() != null && query.minPrice().compareTo(query.maxPrice()) > 0) {
            throw invalid();
        }

        if (query.minExperienceYears() != null && query.maxExperienceYears() != null && query.minExperienceYears() > query.maxExperienceYears()) {
            throw invalid();
        }

        List<Long> resolvedSubjectGradeIds = null;
        if (query.subjectId() != null || query.gradeId() != null) {
            if (query.subjectId() != null && query.gradeId() != null) {
                resolvedSubjectGradeIds = subjectGradeRepository
                        .findBySubjectIdAndGradeId(query.subjectId(), query.gradeId())
                        .map(SubjectGrade::getId)
                        .map(List::of)
                        .orElse(List.of());
            } else if (query.subjectId() != null) {
                resolvedSubjectGradeIds = subjectGradeRepository
                        .findBySubjectId(query.subjectId())
                        .stream()
                        .map(SubjectGrade::getId)
                        .toList();
            } else {
                resolvedSubjectGradeIds = subjectGradeRepository
                        .findByGradeId(query.gradeId())
                        .stream()
                        .map(SubjectGrade::getId)
                        .toList();
            }
        }

        // Build Page
        String field = query.sortBy() == null ? "createdAt" : query.sortBy();
        Sort.Direction direction = query.sortDirection() == null ? Sort.Direction.DESC : Sort.Direction.valueOf(query.sortDirection());
        PageRequest pageRequest = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(direction, field).and(Sort.by(direction, "id")));
        var page = mentorProfileRepository.findAll(
                MentorSearchSpecifications.matching(query, resolvedSubjectGradeIds),
                pageRequest
        );

        // Get Min Price Of Mentor From Mentor Search Paging, Store Map
        Map<Long, BigDecimal> prices = page.isEmpty() ? Map.of() : mentorSubjectRepository.findMinimumActivePrices(
                        page.getContent()
                                .stream()
                                .map(MentorProfile::getId)
                                .toList()
                ).stream()
                .collect(Collectors.toMap(MentorPriceProjection::getMentorId, MentorPriceProjection::getMinPrice));

        var data = page.getContent().stream().map(p -> MentorSearchItemResponse.builder()
                .id(p.getId())
                .userId(p.getUserId())
                .fullName(p.getFullName())
                .avatarUrl(p.getAvatarUrl())
                .gender(p.getGender())
                .headline(p.getHeadline())
                .experienceYears(p.getExperienceYears())
                .currentPosition(p.getCurrentPosition())
                .workplace(p.getWorkplace())
                .education(p.getEducation())
                .major(p.getMajor())
                .meetingType(p.getMeetingType())
                .hometownCityId(p.getHometownCityId())
                .currentDistrictId(p.getCurrentDistrictId())
                .minPrice(prices.get(p.getId()))
                .averageRating(p.getAverageRating())
                .totalReviews(p.getTotalReviews())
                .createdAt(p.getCreatedAt())
                .build()).toList();

        return PageResponse.<MentorSearchItemResponse>builder()
                .data(data)
                .page(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    private ApiException invalid() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid mentor search query.");
    }
}
