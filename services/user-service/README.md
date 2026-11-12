# user-service

Hồ sơ người dùng và học viên của Mentor Matching (MySQL `mm_user`, cổng mặc định `8082`).
Java 21, Spring Boot 4.1.2, Spring Cloud 2025.1.4; package `com.mentormatching.user`.

- `user_id` = `accountId` của auth-service (claim `sub` của JWT); user-service không tự sinh id.
- **Auth-service sở hữu** email đăng nhập, mật khẩu, role và trạng thái tài khoản. user-service chỉ giữ bản sao email và trạng thái lấy từ sự kiện.

## 1. API

| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| GET/PUT | `/api/v1/users/me` | `LEANER` | Xem / tạo-cập nhật hồ sơ cá nhân (`fullName`, `phone`, `userType`) |
| GET/PUT | `/api/v1/users/me/learner-profile` | `LEANER` | Hồ sơ học viên |
| GET | `/api/v1/admin/users?q=&userType=&page=1&size=10` | `ADMIN` | Danh sách người dùng (#66): `q` tìm theo họ tên hoặc SĐT, không phân biệt hoa thường (`%`, `_` là ký tự thường); lọc `userType`; trang từ 1, `size` ≤ 100; mới nhất trước |
| GET | `/api/v1/admin/users/{userId}` | `ADMIN` | Chi tiết hồ sơ, kèm hồ sơ học viên (nếu có) và trạng thái tài khoản |
| PATCH | `/api/v1/admin/users/{userId}/status` | `ADMIN` | Khoá / mở khoá: `{status: ACTIVE\|LOCKED\|DISABLED, reason?}` (xem mục 3) |
| GET | `/internal/v1/users/{userId}` | nội bộ | Tra cứu học viên: `{userId, fullName, email, phone, userType}` (#66) |
| GET | `/internal/v1/users/{userId}/summary` | nội bộ | Như trên (đường dẫn cũ, giữ để tương thích) |
| POST | `/internal/v1/users/batch-summary` | nội bộ | `{userIds: [...]}` (1–100 id) → danh sách summary; id trùng chỉ trả một lần, id không tồn tại bị bỏ qua |

`/internal/**` không đi qua gateway. LEANER/MENTOR gọi API admin nhận 403 ProblemDetail `ACCESS_DENIED`.

Lỗi trả về dạng ProblemDetail (RFC 9457) kèm header `X-Request-ID`. API chỉ nhận Bearer JWT RS256 trong header, nên CSRF tắt có chủ đích.

## 2. Tự tạo hồ sơ từ `AccountRegistered` (#65)

- **Nguồn:** queue riêng `user.account-events` (+ `.dlx`/`.dead`), bind `auth.account.registered` trên exchange chung `mentor.events` (topic, durable, cùng tham số với các service khác).
- **Sự kiện:** do auth-service phát qua outbox (#64), dạng `{eventId, eventType:"AccountRegistered", occurredAt, accountId, email, status, roles}`.
- **Kết quả:** tạo `user_profiles` với `user_id = accountId` và `email` (chuyển về chữ thường).
- **Idempotent:**
  - bản ghi `inbox_events` (khoá chính `eventId`) và việc tạo hồ sơ nằm trong cùng transaction, nên nhận lặp không tạo thêm;
  - người dùng gọi `PUT /me` **trước** khi sự kiện tới: hồ sơ đã có, sự kiện chỉ bổ sung email, **không ghi đè** tên/SĐT;
  - sự kiện và `PUT /me` cùng tạo một lúc: bên chậm hơn được thử lại một lần.
- **Lỗi:**
  - sự kiện sai định dạng (JSON hỏng, thiếu hoặc sai `accountId`) → DLQ, không thử lại;
  - lỗi tạm thời → thử lại có giới hạn;
  - loại sự kiện khác bị bỏ qua.

## 3. Trạng thái tài khoản (#66)

- **Chủ sở hữu là auth-service.** `PATCH /api/v1/admin/users/{id}/status` chuyển tiếp sang `PATCH /api/v1/admin/accounts/{id}/status` của auth-service (#64) **kèm nguyên access token của admin**, nên auth-service tự kiểm tra quyền, thu hồi phiên và phát `AccountStatusChanged`.
- **Lỗi từ auth:**
  - 4xx (404, 409 tự khoá chính mình, ...) → trả về cùng status và mã lỗi;
  - auth không phản hồi hoặc 5xx → 503 `SERVICE_UNAVAILABLE`, không đổi gì phía user-service.
- **Bản sao trạng thái** (`account_status`, `account_status_version`):
  - được cập nhật ngay sau khi auth trả thành công;
  - đồng thời được cập nhật từ sự kiện `AccountRegistered` (version 0) và `AccountStatusChanged` (theo `aggregateVersion`);
  - sự kiện có version cũ hơn tới muộn bị bỏ qua, nên thứ tự không bị đảo.

## 4. Cấu hình

Xem [.env.example](.env.example). Bắt buộc: `DB_PASSWORD`, `AUTH_JWT_AUDIENCE`, `AUTH_JWT_PUBLIC_KEY_LOCATION`. Biến mới:

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `RABBITMQ_HOST` / `RABBITMQ_PORT` / `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | `localhost` / `5672` / `guest` / `guest` | Cùng tên biến với payment-service |
| `USER_MESSAGING_ENABLED` | `true` | `false`: không nhận sự kiện (hồ sơ vẫn tạo được qua `PUT /me`) |
| `USER_EVENTS_QUEUE` / `USER_EVENTS_ROUTING_KEYS` | `user.account-events` / `auth.account.#` | Queue riêng và pattern bind (nhận `AccountRegistered` và `AccountStatusChanged`) |
| `USER_EVENTS_MAX_RETRIES` | `4` | Số lần thử lại khi lỗi tạm thời |
| `AUTH_SERVICE_URL` / `AUTH_SERVICE_TIMEOUT` | `http://localhost:8081` / `5s` | auth-service cho PATCH trạng thái (#66) |

Schema do Flyway quản lý:
- `V1` tạo bảng hồ sơ;
- `V2` thêm cột `email` và bảng `inbox_events`;
- `V3` thêm `account_status`, `account_status_version` (#66).

## 5. Build và test

```bash
./mvnw clean verify   # cần Docker: integration test dùng Testcontainers MySQL 8.4 + RabbitMQ
./mvnw test           # chỉ unit/slice test
```

| Lớp | Số test | Nội dung |
|---|---|---|
| `ProblemContractTest` | 18 | Hợp đồng lỗi ProblemDetail, request id, 401 Bearer, 403, API stateless không cần CSRF token |
| `UserProvisioningIT` | 6 | `AccountRegistered` tạo hồ sơ (email chữ thường) và `GET /me` dùng được ngay; gửi lại chỉ tạo 1 hồ sơ; **4 luồng đồng thời → tạo 1 lần**; `PUT /me` trước sự kiện giữ dữ liệu người dùng; sự kiện hỏng → DLQ; summary nội bộ có email, 404 / 400 đúng |
| `AdminUsersIT` | 6 | Tìm theo tên/SĐT không phân biệt hoa thường, lọc `userType`, phân trang; chi tiết; LEANER/MENTOR → 403, không token → 401, tham số sai → 400; PATCH chuyển cho auth giả lập kèm token admin, 404 giữ nguyên, auth lỗi → 503; sự kiện trạng thái theo `aggregateVersion` (bỏ qua sự kiện cũ); API nội bộ đơn lẻ và batch |

## 6. Chạy

Cần MySQL (database `mm_user`) và RabbitMQ. Đặt các biến ở trên rồi chạy:

```bash
./mvnw spring-boot:run
```
