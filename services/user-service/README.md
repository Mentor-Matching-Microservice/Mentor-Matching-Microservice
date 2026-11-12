# user-service

Hồ sơ người dùng và học viên của Mentor Matching (MySQL `mm_user`, cổng mặc định `8082`).
Java 21, Spring Boot 4.1.2, Spring Cloud 2025.1.4; package `com.mentormatching.user`.

- `user_id` = `accountId` của auth-service (claim `sub` của JWT); user-service không tự sinh id.
- **Auth-service sở hữu** email đăng nhập, mật khẩu, role và trạng thái tài khoản. user-service chỉ giữ bản sao email lấy từ sự kiện.

## 1. API

| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| GET/PUT | `/api/v1/users/me` | `LEANER` | Xem / tạo-cập nhật hồ sơ cá nhân (`fullName`, `phone`, `userType`) |
| GET/PUT | `/api/v1/users/me/learner-profile` | `LEANER` | Hồ sơ học viên |
| GET | `/internal/v1/users/{userId}/summary` | nội bộ | `{userId, fullName, email, phone}` cho service khác (không đi qua gateway) |

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

## 3. Cấu hình

Xem [.env.example](.env.example). Bắt buộc: `DB_PASSWORD`, `AUTH_JWT_AUDIENCE`, `AUTH_JWT_PUBLIC_KEY_LOCATION`. Biến mới:

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `RABBITMQ_HOST` / `RABBITMQ_PORT` / `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | `localhost` / `5672` / `guest` / `guest` | Cùng tên biến với payment-service |
| `USER_MESSAGING_ENABLED` | `true` | `false`: không nhận sự kiện (hồ sơ vẫn tạo được qua `PUT /me`) |
| `USER_EVENTS_QUEUE` / `USER_EVENTS_ROUTING_KEYS` | `user.account-events` / `auth.account.registered` | Queue riêng và pattern bind |
| `USER_EVENTS_MAX_RETRIES` | `4` | Số lần thử lại khi lỗi tạm thời |

Schema do Flyway quản lý:
- `V1` tạo bảng hồ sơ;
- `V2` thêm cột `email` và bảng `inbox_events`.

## 4. Build và test

```bash
./mvnw clean verify   # cần Docker: integration test dùng Testcontainers MySQL 8.4 + RabbitMQ
./mvnw test           # chỉ unit/slice test
```

| Lớp | Số test | Nội dung |
|---|---|---|
| `ProblemContractTest` | 18 | Hợp đồng lỗi ProblemDetail, request id, 401 Bearer, 403, API stateless không cần CSRF token |
| `UserProvisioningIT` | 6 | `AccountRegistered` tạo hồ sơ (email chữ thường) và `GET /me` dùng được ngay; gửi lại chỉ tạo 1 hồ sơ; **4 luồng đồng thời → tạo 1 lần**; `PUT /me` trước sự kiện giữ dữ liệu người dùng; sự kiện hỏng → DLQ; summary nội bộ có email, 404 / 400 đúng |

## 5. Chạy

Cần MySQL (database `mm_user`) và RabbitMQ. Đặt các biến ở trên rồi chạy:

```bash
./mvnw spring-boot:run
```
