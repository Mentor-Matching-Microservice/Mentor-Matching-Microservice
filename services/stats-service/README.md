# stats-service

Thống kê cho trang **Admin Dashboard** (`/admin`) và **Reports** (`/admin/reports`) — issue #82. Thay module `stats` của dự án cũ (đọc thẳng 4 bảng) bằng cách **tổng hợp từ sự kiện nghiệp vụ** trên RabbitMQ.

## 1. Vì sao tổng hợp từ sự kiện

- Tiêu chí #82 yêu cầu: không có truy vấn SQL/Mongo nào đọc sang database của service khác.
- Issue gợi ý thêm endpoint thống kê nội bộ vào user, mentor, booking và payment. Cách đó phải sửa service của người khác, nên **không làm** ở đây.
- Thay vào đó, stats-service tự nhận các sự kiện mà các service **đang publish sẵn** lên exchange chung `mentor.events`, cộng dồn vào MongoDB `mm_stats` của riêng nó.
- Khi truy vấn, service chỉ đọc số đã tổng hợp, không gọi service nào khác. Vì vậy không có tình huống "một service thống kê phản hồi chậm"; cờ `partialData` dùng để báo **chỉ số chưa có nguồn dữ liệu** (xem mục 3).
- Không sửa code của service nào khác.

## 2. Thông tin chính

| Mục | Giá trị |
|---|---|
| Cổng | `8090` (`SERVER_PORT`) — 8080–8089 đã có service |
| Database | MongoDB **replica set**, database `mm_stats` (`daily_stats`, `processed_events`, `metric_sources`, `payment_refund_totals`) |
| Broker | Exchange dùng chung `mentor.events` → queue riêng `stats.domain-events` (+ `.dlx`/`.dead`) |
| Xác thực | Bearer JWT **RS256** do auth-service ký; API chỉ cho `ROLE_ADMIN` |
| Stack | Java 21, Spring Boot 4.1.2, Spring Cloud 2025.1.4 (cùng nền với notification-service) |

## 3. Nguồn dữ liệu của từng chỉ số

| Chỉ số (field) | Sự kiện nguồn | Trạng thái trên `develop` |
|---|---|---|
| `totalRevenue`, `successfulPayments`, timeseries `revenue` | `PaymentSucceeded` (`payment.events`) | ✅ payment-service đang publish |
| `totalRefunded`, `netRevenue`, timeseries `refunded` | `PaymentRefunded` (`refundedAmount` là tổng đã hoàn; chỉ cộng phần mới) | ✅ |
| `confirmedBookings`, timeseries `confirmedBookings` | `BookingConfirmed` (`booking.lifecycle`) | ✅ booking-service đang publish |
| `cancelledBookings`, `rejectedBookings` | `BookingCancelled`, `BookingRejected` | ✅ |
| `newUsersCount` | `AccountRegistered` | ⏳ auth-service chưa publish (#64) |
| `newMentorsCount` | `MentorApproved` | ⏳ mentor-service chưa publish (#69) |
| `totalBookings`, timeseries `bookingsCount` | `BookingCreated` | ⏳ booking-service chưa đưa lên RabbitMQ |
| `completedBookings`, `completionRate` | `BookingCompleted` | ⏳ booking-service chưa đưa lên RabbitMQ |

- **Chỉ số ⏳:**
  - trả về `0`, kèm `partialData: true` và tên field trong `unavailableMetrics`;
  - **tự có dữ liệu** ngay khi stats-service nhận sự kiện nguồn đầu tiên, không cần sửa gì thêm;
  - định dạng sự kiện mong đợi giống hợp đồng ghi trong README của notification-service.
- **Chia ngày theo giờ Việt Nam** (`STATS_ZONE`), dựa trên `occurredAt` của sự kiện.
- **Doanh thu và hoàn tiền** chỉ cộng các khoản bằng `STATS_CURRENCY` (mặc định `VND`). Khoản thanh toán bằng tiền khác vẫn được đếm.

## 4. API

| Method | Đường dẫn | Quyền |
|---|---|---|
| GET | `/api/v1/admin/stats/overview?from=yyyy-MM-dd&to=yyyy-MM-dd` | `ROLE_ADMIN` |
| GET | `/api/v1/admin/stats/timeseries?from=yyyy-MM-dd&to=yyyy-MM-dd` | `ROLE_ADMIN` |

- **Khoảng ngày:** mặc định 30 ngày gần nhất, giống dự án cũ; tối đa 366 ngày.
  - `from > to` → 400 `INVALID_DATE_RANGE`.
  - Khoảng quá dài → 400 `DATE_RANGE_TOO_LARGE`.
  - Sai định dạng ngày → 400.
- **Lỗi xác thực:** không có token → 401; không phải ADMIN → 403. Mọi lỗi dùng dạng ProblemDetail như các service khác.
- **Ngày không có phát sinh** vẫn có một điểm với giá trị `0`, để biểu đồ Recharts ổn định.

Overview (8 field đầu giữ tên của dự án cũ):

```json
{"from":"2026-11-01","to":"2026-11-10","newUsersCount":0,"newMentorsCount":0,"totalBookings":0,
 "completedBookings":0,"completionRate":0.0,"totalRevenue":500000,
 "confirmedBookings":2,"cancelledBookings":1,"rejectedBookings":0,"successfulPayments":2,
 "totalRefunded":300000,"netRevenue":200000,"currency":"VND",
 "partialData":true,"unavailableMetrics":["newUsersCount","newMentorsCount","totalBookings","completedBookings","completionRate"]}
```

Timeseries: `{from, to, currency, partialData, unavailableMetrics, points: [{date, bookingsCount, confirmedBookings, revenue, refunded}]}`. Lưu ý: `date`, `bookingsCount` và `revenue` giữ tên cũ, nhưng danh sách điểm giờ nằm trong `points` (để đi kèm cờ `partialData`).

## 5. Độ chính xác

- `processed_events` (`_id = eventId`) và `$inc` nằm trong **cùng transaction MongoDB**, nên sự kiện gửi lại không bị cộng hai lần. Hai lần giao đồng thời bị MongoDB huỷ một bên (TransientTransactionError); service thử lại và kết quả là DUPLICATE.
- `PaymentRefunded` mang tổng đã hoàn, nên chỉ cộng phần tăng thêm so với lần trước. Sự kiện cũ tới muộn không làm giảm số đã cộng.
- **Lỗi:** sự kiện sai định dạng vào DLQ, không retry. Lỗi tạm thời được retry có giới hạn rồi mới vào DLQ. Loại sự kiện không liên quan được ack và bỏ qua.

## 6. Cấu hình (biến môi trường)

| Biến | Bắt buộc | Mặc định | Ý nghĩa |
|---|---|---|---|
| `SERVER_PORT` | | `8090` | Cổng HTTP |
| `MONGODB_URI` | | `mongodb://localhost:27017/mm_stats?replicaSet=rs0` | Replica set (transaction) |
| `AUTH_JWT_AUDIENCE` | ✅ | — | Giống auth-service |
| `AUTH_JWT_PUBLIC_KEY_LOCATION` | ✅ | — | Ví dụ `file:../auth-service/jwt-keys/public.pem` |
| `AUTH_JWT_ISSUER` | | `auth-service` | |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` / `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | | `localhost` / `5672` / `guest` / `guest` | Cùng tên biến với payment-service |
| `STATS_MESSAGING_ENABLED` | | `true` | `false` để chạy không cần RabbitMQ |
| `STATS_EVENTS_QUEUE` | | `stats.domain-events` | Queue riêng |
| `STATS_EVENTS_ROUTING_KEYS` | | `booking.#,payment.#,auth.#,mentor.#` | |
| `STATS_EVENTS_MAX_RETRIES` | | `4` | 0–10 |
| `STATS_ZONE` | | `Asia/Ho_Chi_Minh` | Múi giờ chia ngày |
| `STATS_CURRENCY` | | `VND` | Đơn vị tiền được cộng doanh thu |
| `STATS_CORS_ALLOWED_ORIGINS` | | `http://localhost:5173` | |
| `EUREKA_ENABLED` / `EUREKA_URL` | | `false` / `http://localhost:8761/eureka/` | discovery-service cần `http://<user>:<password>@host:8761/eureka/` |

Thiếu `AUTH_JWT_AUDIENCE` hoặc `AUTH_JWT_PUBLIC_KEY_LOCATION` thì service dừng ngay khi khởi động. Mẫu biến môi trường: [.env.example](.env.example).

## 7. Chạy và kiểm thử

```bash
# từ services/stats-service; cần MongoDB replica set + RabbitMQ (xem README notification-service mục 8)
export AUTH_JWT_AUDIENCE=<giống auth-service>
export AUTH_JWT_PUBLIC_KEY_LOCATION=file:../auth-service/jwt-keys/public.pem
./mvnw spring-boot:run

./mvnw clean verify   # cần Docker: integration test dùng Testcontainers MongoDB + RabbitMQ
```

| Loại | Lớp | Số test | Nội dung |
|---|---|---|---|
| Unit | `JwtConfigurationTest` | 15 | Hợp đồng JWT RS256 (giống các service khác) |
| Unit | `ProblemResponsesTest` | 18 | Hợp đồng lỗi ProblemDetail |
| Unit | `StatsConfigurationTest` | 3 | Ánh xạ sự kiện → chỉ số, giá trị mặc định, không có secret |
| IT | `AdminStatsIT` | 9 | Doanh thu/hoàn tiền (gồm hoàn nhiều lần, tiền khác VND), booking; chỉ số tự có dữ liệu khi nguồn bắt đầu publish; chia ngày theo giờ VN; kỳ trống trả 0; gửi lại không đếm trùng; **4 luồng đồng thời → đếm 1 lần**; DLQ; kiểm tra khoảng ngày; chỉ ADMIN |

Postman: [postman/stats-service.postman_collection.json](postman/stats-service.postman_collection.json), cần `adminToken` (ROLE_ADMIN) và `userToken` (không phải admin).

## 8. Giới hạn và việc tiếp theo

- **Chỉ đếm sự kiện từ lúc stats-service bắt đầu chạy.** Dữ liệu có từ trước (booking, thanh toán cũ) không được tính lại, vì không đọc DB của service khác. Nếu cần, có thể bổ sung bước replay outbox phía publisher.
- **Gateway:** route `/api/v1/admin/stats/**` → `lb://stats-service` thuộc #59 (service của người khác).
- **Docker Compose:** dùng chung MongoDB replica set và RabbitMQ (#57).
