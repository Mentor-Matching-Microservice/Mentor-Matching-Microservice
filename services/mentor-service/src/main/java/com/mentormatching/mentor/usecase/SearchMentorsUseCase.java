package com.mentormatching.mentor.usecase;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

import com.mentormatching.mentor.dto.query.SearchMentorsQuery;
import com.mentormatching.mentor.dto.response.*;
import com.mentormatching.mentor.entity.MentorProfile;
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
    private final Validator validator;

    /**
     * Khởi tạo use case tìm kiếm.
     *
     * @param mentorProfileRepository  repository truy vấn hồ sơ và phân trang
     * @param mentorSubjectRepository  repository tổng hợp giá các môn đang hoạt động
     * @param validator validator kiểm tra query cả khi caller không đi qua HTTP
     */
    public SearchMentorsUseCase(MentorProfileRepository mentorProfileRepository, MentorSubjectRepository mentorSubjectRepository, Validator validator) {
        this.mentorProfileRepository = mentorProfileRepository;
        this.mentorSubjectRepository = mentorSubjectRepository;
        this.validator = validator;
    }

    /**
     * Tìm kiếm với sort mặc định createdAt DESC và ID làm tiêu chí phụ cùng chiều.
     * Các điều kiện môn và khoảng giá phải khớp cùng một môn đang hoạt động.
     * Giá trả về là giá thấp nhất của tất cả môn hoạt động, không chỉ môn khớp bộ lọc.
     *
     * @param query bộ lọc và phân trang; page/size bắt buộc, query không được null
     * @return danh sách DTO cùng tổng số bản ghi/trang sau khi lọc
     * @throws ApiException lỗi 400 INVALID_REQUEST nếu query sai constraint hoặc khoảng min/max đảo ngược
     */
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

        // Build Page
        String field = query.sortBy() == null ? "createdAt" : query.sortBy();
        Sort.Direction direction = query.sortDirection() == null ? Sort.Direction.DESC : Sort.Direction.valueOf(query.sortDirection());
        PageRequest pageRequest = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(direction, field).and(Sort.by(direction, "id")));
        var page = mentorProfileRepository.findAll(MentorSearchSpecifications.matching(query), pageRequest);

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
                .workplace(p.getWorkplace()).education(p.getEducation())
                .major(p.getMajor())
                .meetingType(p.getMeetingType())
                .hometownCityId(p.getHometownCityId())
                .currentDistrictId(p.getCurrentDistrictId())
                .minPrice(prices.get(p.getId()))
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

    /**
     * Tạo lỗi đầu vào với mã ổn định, không đưa nội dung query vào thông báo.
     *
     * @return exception 400 INVALID_REQUEST
     */
    private ApiException invalid() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid mentor search query.");
    }
}
