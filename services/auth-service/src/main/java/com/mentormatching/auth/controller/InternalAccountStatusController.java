package com.mentormatching.auth.controller;
import com.mentormatching.auth.dto.query.AccountStatusesQuery;
import com.mentormatching.auth.dto.response.AccountStatusesResponse;
import com.mentormatching.auth.service.GetAccountStatusesUseCase;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
/**
 * Endpoint đọc snapshot tài khoản dành cho service nội bộ, chỉ đăng ký khi bật account-sync.
 * InternalAccountSecurityConfiguration chịu trách nhiệm xác thực token dùng chung.
 */
@RestController
@ConditionalOnProperty(name = "auth.internal.account-sync.enabled", havingValue = "true")
public class InternalAccountStatusController {
    private final GetAccountStatusesUseCase useCase;
    /**
     * Khởi tạo controller mỏng chuyển yêu cầu sang use case.
     * @param useCase use case đọc trạng thái tài khoản
     */
    public InternalAccountStatusController(GetAccountStatusesUseCase useCase) { this.useCase = useCase; }
    /**
     * Nhận batch UUID đã được xác thực bởi security chain nội bộ.
     * @param query danh sách account cần lấy trạng thái, có Bean Validation ở tầng HTTP
     * @return các snapshot tìm thấy; account không tồn tại không xuất hiện trong response
     */
    @PostMapping("/internal/accounts/statuses")
    public AccountStatusesResponse statuses(@Valid @RequestBody AccountStatusesQuery query) { return useCase.execute(query); }
}
