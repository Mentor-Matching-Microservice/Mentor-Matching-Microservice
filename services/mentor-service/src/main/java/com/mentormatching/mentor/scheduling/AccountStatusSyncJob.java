package com.mentormatching.mentor.scheduling;

import com.mentormatching.mentor.client.AuthAccountStatusClient;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import com.mentormatching.mentor.service.ApplyAccountStatusesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Đồng bộ trạng thái tài khoản theo lịch khi được bật bằng cấu hình.
 * Duyệt mentor theo ID tăng dần, tối đa 100 hồ sơ mỗi batch; mỗi lượt bắt đầu lại từ đầu.
 */
@Component
@ConditionalOnProperty(name = "mentor.account-sync.enabled", havingValue = "true")
public class AccountStatusSyncJob {

    private static final Logger log = LoggerFactory.getLogger(AccountStatusSyncJob.class);
    private final MentorProfileRepository repository;
    private final AuthAccountStatusClient client;
    private final ApplyAccountStatusesService apply;

    /**
     * Khởi tạo job và các thành phần đọc, gọi HTTP, ghi snapshot.
     * @param repository repository đọc các hồ sơ mentor theo batch
     * @param client client gọi endpoint nội bộ của auth
     * @param apply service áp dụng snapshot qua Spring proxy để mở transaction riêng
     */
    public AccountStatusSyncJob(MentorProfileRepository repository, AuthAccountStatusClient client,
                                ApplyAccountStatusesService apply) {
        this.repository = repository;
        this.client = client;
        this.apply = apply;
    }

    /**
     * Thực hiện một lượt đồng bộ; HTTP chạy ngoài transaction ghi database.
     * Mặc định chạy sau 10 giây, rồi chờ 60 giây sau khi lượt trước kết thúc.
     * Tài khoản thiếu trong response được giữ nguyên. Khi gặp RuntimeException, dừng lượt hiện tại,
     * giữ các batch đã commit và thử lại từ đầu ở lượt sau; không log token hoặc HTTP body.
     */
    @Scheduled(fixedDelayString = "${mentor.account-sync.delay-ms:60000}",
            initialDelayString = "${mentor.account-sync.initial-delay-ms:10000}")
    public void synchronize() {
        // HTTP calls run outside DB transactions. Each applied batch has its own transaction.
        log.info("Syncing accounts from Mentor starting:...");
        long afterId = 0;
        try {
            while (true) {
                var profiles = repository.findByIdGreaterThanOrderByIdAsc(afterId, PageRequest.of(0, 100));
                if (profiles.isEmpty()) return;
                var ids = profiles.stream().map(p -> p.getUserId()).toList();
                var response = client.fetch(ids);
                apply.execute(response);
                if (response.accounts().size() < ids.size()) {
                    log.warn("Auth sync returned {} of {} requested accounts", response.accounts().size(), ids.size());
                }
                afterId = profiles.getLast().getId();
            }
        } catch (RuntimeException ex) {
            // Do not log HTTP bodies, headers or credentials. Next scheduled run retries from the start.
            log.warn("Account status sync failed; retry on next run (type={})", ex.getClass().getSimpleName());
        }
    }
}
