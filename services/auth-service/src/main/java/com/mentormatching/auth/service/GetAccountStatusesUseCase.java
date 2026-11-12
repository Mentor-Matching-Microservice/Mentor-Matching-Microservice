package com.mentormatching.auth.service;
import com.mentormatching.auth.dto.query.AccountStatusesQuery;
import com.mentormatching.auth.dto.response.*;
import com.mentormatching.auth.repository.AccountRepository;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mentormatching.auth.exception.ApiException;
import com.mentormatching.auth.exception.ErrorCode;
/**
 * Đọc trạng thái và version tài khoản để cung cấp cho đồng bộ nội bộ.
 * Không trả thông tin đăng nhập hoặc credentials; endpoint gọi service phải được bảo vệ.
 */
@Service
@Transactional(readOnly = true)
public class GetAccountStatusesUseCase {
    private final AccountRepository accounts;
    private final Validator validator;
    /**
     * Khởi tạo use case đọc snapshot tài khoản.
     * @param accounts repository tài khoản do auth sở hữu
     * @param validator validator kiểm tra query khi gọi từ HTTP hoặc caller khác
     */
    public GetAccountStatusesUseCase(AccountRepository accounts, Validator validator) {
        this.accounts = accounts; this.validator = validator;
    }
    /**
     * Đọc snapshot cho các UUID yêu cầu trong transaction read-only qua Spring proxy.
     * ID không tồn tại bị bỏ khỏi kết quả; thứ tự response không được bảo đảm giống thứ tự request.
     *
     * @param query danh sách từ 1 đến 100 UUID không null
     * @return batch gồm UUID, status và version hiện tại của các account tìm được
     * @throws ApiException lỗi 400 INVALID_REQUEST nếu query không hợp lệ
     */
    public AccountStatusesResponse execute(AccountStatusesQuery query) {
        if (query == null || !validator.validate(query).isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST, "Invalid account status query");
        }
        return AccountStatusesResponse.builder().accounts(accounts.findAllById(query.userIds()).stream()
            .map(a -> AccountStatusSnapshot.builder().userId(a.getId()).status(a.getStatus()).version(a.getVersion()).build())
            .toList()).build();
    }
}
