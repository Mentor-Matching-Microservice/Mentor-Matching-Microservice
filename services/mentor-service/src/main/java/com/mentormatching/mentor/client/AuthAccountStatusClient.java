package com.mentormatching.mentor.client;

import java.util.List;
import java.util.UUID;

import com.mentormatching.mentor.dto.query.AccountStatusesQuery;
import com.mentormatching.mentor.dto.response.AccountStatusesResponse;
import org.springframework.web.client.RestClient;

/**
 * Gọi endpoint nội bộ của auth-service để đọc snapshot trạng thái tài khoản.
 * Client không ghi database; việc validate đầy đủ và áp dụng version thuộc service đồng bộ.
 */
public class AuthAccountStatusClient {
    private final RestClient client;

    /**
     * Khởi tạo client từ RestClient đã cấu hình base URL, token và timeout.
     * @param client HTTP client dùng chung cho các lần gọi
     */
    public AuthAccountStatusClient(RestClient client) {
        this.client = client;
    }

    /**
     * Gửi danh sách UUID tới POST /internal/accounts/statuses.
     * Tài khoản không tồn tại có thể bị bỏ khỏi response; không suy ra trạng thái từ việc thiếu dữ liệu.
     *
     * @param userIds danh sách UUID cần đọc, caller cung cấp từ 1 đến 100 ID không null
     * @return snapshot do auth trả về, có thể chứa ít account hơn số ID đã gửi
     * @throws IllegalStateException nếu response thiếu danh sách hoặc chứa account ngoài yêu cầu
     * @throws org.springframework.web.client.RestClientException nếu HTTP hoặc việc đọc response thất bại
     */
    public AccountStatusesResponse fetch(List<UUID> userIds) {
        var response = client.post()
                .uri("/internal/accounts/statuses")
                .body(AccountStatusesQuery.builder()
                        .userIds(List.copyOf(userIds))
                        .build())
                .retrieve()
                .body(AccountStatusesResponse.class);
        if (response == null
                || response.accounts() == null
                || response.accounts().stream().anyMatch(a -> a == null || !userIds.contains(a.userId()))) {
            throw new IllegalStateException("Invalid auth account status response");
        }
        return response;
    }
}
