package com.mentormatching.mentor.service;

import com.mentormatching.mentor.dto.query.GetAdminMentorVerificationsQuery;
import com.mentormatching.mentor.dto.response.AdminMentorVerificationItemResponse;
import com.mentormatching.mentor.dto.response.PageResponse;
import com.mentormatching.mentor.entity.MentorVerification;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorVerificationRepository;
import jakarta.validation.Validator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Liệt kê verification cho admin theo trạng thái và phân trang tại database.
 */
@Service
@PreAuthorize("hasRole('ADMIN')")
@Transactional(readOnly = true)
public class GetAdminMentorVerificationsUseCase {

    private final MentorVerificationRepository repository;
    private final Validator validator;

    public GetAdminMentorVerificationsUseCase(MentorVerificationRepository repository, Validator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    /**
     * Sắp xếp theo createdAt giảm dần, ID giảm dần để phân trang ổn định khi trùng thời điểm.
     *
     * @param query bộ lọc và phân trang đã được chuẩn hóa trong record
     * @return trang tóm tắt verification, không chứa ảnh hoặc số giấy tờ
     * @throws ApiException 400 nếu query sai constraint hoặc null
     */
    public PageResponse<AdminMentorVerificationItemResponse> execute(GetAdminMentorVerificationsQuery query) {
        if (query == null || !validator.validate(query).isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid mentor verification query.");
        }

        var pageable = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(Sort.Direction.DESC, "createdAt", "id")
        );

        Page<MentorVerification> page = query.status() == null ? repository.findAll(pageable) : repository.findByVerificationStatus(query.status(), pageable);

        var items = page.getContent().stream().map(v -> AdminMentorVerificationItemResponse.builder()
                .id(v.getId())
                .mentorId(v.getMentorId())
                .fullName(v.getFullName())
                .verificationStatus(v.getVerificationStatus())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build()).toList();

        return PageResponse.<AdminMentorVerificationItemResponse>builder()
                .page(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalPages(page.getTotalPages())
                .totalItems(page.getTotalElements())
                .data(items)
                .build();
    }
}
