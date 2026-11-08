package com.mentormatching.mentor.service;

import com.mentormatching.mentor.dto.response.AccountStatusesResponse;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Áp dụng bản sao account status từ auth vào những hồ sơ mentor đã tồn tại.
 * Không tạo profile mới; trạng thái gốc vẫn thuộc auth-service.
 */
@Service
public class ApplyAccountStatusesService {
    private final MentorProfileRepository mentorProfileRepository;
    private final Validator validator;

    /**
     * Khởi tạo service cập nhật trạng thái theo version.
     * @param mentorProfileRepository repository thực hiện cập nhật có điều kiện theo UUID và version
     * @param validator validator kiểm tra cấu trúc snapshot trước khi ghi
     */
    public ApplyAccountStatusesService(MentorProfileRepository mentorProfileRepository, Validator validator) {
        this.mentorProfileRepository = mentorProfileRepository;
        this.validator = validator;
    }

    /**
     * Validate cả batch rồi cập nhật các snapshot trong một transaction qua Spring proxy.
     * Chỉ nhận version mới hơn bản sao hiện có, hoặc khởi tạo bản sao chưa có version.
     * Snapshot cũ/trùng version và account không có mentor tương ứng không làm thay đổi dữ liệu.
     *
     * @param response batch tối đa 100 snapshot, không được null hoặc có UUID trùng nhau
     * @return số hồ sơ thực sự được cập nhật
     * @throws ApiException lỗi 400 INVALID_REQUEST nếu batch không hợp lệ
     */
    @Transactional
    public int execute(AccountStatusesResponse response) {
        if (response == null || !validator.validate(response).isEmpty()
                || response.accounts().stream().map(a -> a.userId()).distinct().count() != response.accounts().size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid account status snapshot.");
        }
        int updated = 0;
        for (var account : response.accounts()) {
            updated += mentorProfileRepository.applyAccountStatus(account.userId(), account.status(), account.version());
        }
        return updated;
    }
}
