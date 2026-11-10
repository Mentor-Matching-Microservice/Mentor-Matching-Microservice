# notification-service

Service quản lý **hộp thư thông báo** của người dùng (MongoDB `mm_notification`), **nhận sự kiện nghiệp vụ** qua RabbitMQ và **đẩy thông báo realtime** qua STOMP WebSocket.

## 1. Trạng thái

| Phạm vi | Issue | Trạng thái |
|---|---|---|
| Khởi tạo project, MongoDB replica set + transaction, STOMP broker, Eureka client, RS256 resource server, ProblemDetail, Testcontainers MongoDB | #62 (phần notification) | ✅ |
| REST API hộp thư, xác thực STOMP ở frame CONNECT, consumer `AccountRegistered` / `MentorApproved` / `MentorRejected`, chống trùng theo `eventId` | #73 | ✅ Nhánh `feat/notification-core-73` |
| Sự kiện Booking / Payment / Review, `delivery_attempts`, retry khi đẩy realtime lỗi | #81 | ✅ Nhánh `feat/notification-domain-events-81` |

Phần payment-service / review-service (MSSQL) của #62 do người phụ trách các service đó làm, không nằm ở đây.

## 2. Thông tin chính

| Mục | Giá trị |
|---|---|
| Cổng | `8089` (`SERVER_PORT`) — xem [mục 11](#11-giới-hạn-và-việc-tiếp-theo) |
| Database | MongoDB **replica set**, database `mm_notification` (collection `notifications`, `inbox_events`) |
| Broker | RabbitMQ: exchange dùng chung `mentor.events`, queue riêng `notification.domain-events` |
| Stack | Java 21, Spring Boot 4.1.2, Spring Cloud 2025.1.4 |
| Xác thực | Bearer JWT **RS256** do auth-service ký; chỉ cần public key |
| Realtime | STOMP qua WebSocket `/ws/notifications`, đích người dùng `/user/queue/notifications` |
| Discovery | Eureka client, tắt mặc định (`EUREKA_ENABLED=false`) |

## 3. Cấu trúc

```
src/main/java/com/mentormatching/notification
├── NotificationServiceApplication.java
├── config        JwtConfiguration, SecurityConfiguration, CorsConfig, MongoConfiguration,
│                 WebSocketConfiguration, ClockConfiguration
├── controller    NotificationController             # REST /api/v1/notifications
├── domain        Notification, NotificationType, InboxEvent, DeliveryAttempt
├── dto           ApiResponse, PageResponse, NotificationResponse
├── client        BookingParticipantsClient       # tra học viên/mentor qua API nội bộ có sẵn của booking (chỉ gọi)
├── event         DomainEvent (envelope), EventNotificationMapper + AccountEventMapper,
│                 MentorVerificationEventMapper, BookingEventMapper, PaymentEventMapper, ReviewEventMapper
├── messaging     MessagingConfiguration (queue, DLQ, retry), MessagingProperties, DomainEventListener
├── repository    NotificationRepository, InboxEventRepository
├── service       NotificationService (hộp thư), NotificationIngestionService (sự kiện → thông báo),
│                 RealtimeNotificationPublisher (STOMP sau commit)
├── websocket     StompAuthChannelInterceptor        # JWT ở frame CONNECT
├── exception     Hợp đồng lỗi ProblemDetail dùng chung với user-service
└── security      ProblemAuthenticationEntryPoint, ProblemAccessDeniedHandler
```

## 4. REST API (#73)

Tất cả cần `Authorization: Bearer <access token>`; userId = `sub` của token. Đường dẫn và hình dạng response giữ như dự án cũ để frontend dùng lại các hook hiện có.

| Method | Đường dẫn | `data` trả về |
|---|---|---|
| GET | `/api/v1/notifications?page=1&size=10` | `PageResponse` (`page` bắt đầu từ 1, `size` 1–100), mới nhất trước |
| GET | `/api/v1/notifications/unread-count` | số thông báo chưa đọc |
| PATCH | `/api/v1/notifications/{id}/read` | thông báo sau khi đánh dấu (gọi lại không đổi `readAt`) |
| POST | `/api/v1/notifications/mark-all-read` | số thông báo vừa chuyển sang đã đọc |
| DELETE | `/api/v1/notifications/{id}` | `null` |

Response thành công dùng vỏ chung `{status, code, success, message, data}`. Một thông báo:

```json
{"id":"<uuid>","userId":"<uuid>","type":"MENTOR_APPROVED","title":"…","message":"…","isRead":false,
 "referenceType":"MENTOR","referenceId":"17","createdAt":"2026-11-10T10:00:00.123Z","readAt":null}
```

- Thông báo của người khác được xử lý **như không tồn tại**: trả 404 `NOTIFICATION_NOT_FOUND`, không trả 403, để không lộ việc nó có tồn tại.
- `id` không phải UUID → 400; `page`/`size` ngoài giới hạn → 400 `VALIDATION_ERROR`.

## 5. Realtime (STOMP)

| Bước | Quy tắc |
|---|---|
| Handshake `GET /ws/notifications` | Không cần token (trình duyệt không gửi được header khi mở WebSocket). Chỉ origin trong `NOTIFICATION_CORS_ALLOWED_ORIGINS`; origin lạ → 403 |
| `CONNECT` | **Bắt buộc** native header `Authorization: Bearer <JWT>`, kiểm tra giống hệt HTTP (RS256, iss, aud, exp, sub UUID, roles). Thiếu/sai → frame ERROR và đóng kết nối. Principal = userId |
| `SUBSCRIBE` | Chỉ `/user/queue/notifications`; đích khác → ERROR và đóng kết nối |
| `SEND` | Bị chặn (kênh một chiều server → client) |
| Heartbeat | 10 giây hai chiều |

Frontend cũ (`useNotificationWebsocket.ts`, `@stomp/stompjs`) dùng được nguyên trạng: nó gửi token trong `connectHeaders` và subscribe `/user/queue/notifications`.

Thông báo chỉ được đẩy **sau khi transaction MongoDB commit**. Đẩy realtime lỗi không làm mất thông báo, vì client vẫn thấy khi tải lại danh sách.

## 6. Nhận sự kiện (RabbitMQ)

```
mentor.events (topic, dùng chung) --[auth.#, mentor.#, booking.#, payment.#, review.#]--> notification.domain-events
                                                              | sai định dạng / hết số lần thử
                                                              v
                             notification.domain-events.dlx --> notification.domain-events.dead
```

- **Không ảnh hưởng service khác:**
  - notification chỉ khai báo queue, DLX và DLQ **của riêng nó**;
  - exchange `mentor.events` được khai báo với đúng tham số booking, payment và review đang dùng (topic, durable, không auto-delete);
  - không sửa và không tiêu thụ queue của service khác.
- **Định dạng envelope:** dạng phẳng, giống booking, payment và review đang publish: `{"eventId", "eventType", "occurredAt", …các trường riêng}`. Body được đọc thẳng dưới dạng byte JSON.
- **Chống trùng:**
  - `inbox_events` có `_id = eventId` và được ghi **trong cùng transaction** với thông báo.
  - Thêm unique index `(sourceEventId, recipientUserId)` làm lớp bảo vệ thứ hai.
  - Nếu hai lần giao cùng một sự kiện tới đồng thời, MongoDB báo `WriteConflict`. Service thử lại transaction và kết quả là DUPLICATE.
  - Bản ghi inbox tự xoá sau 30 ngày (TTL).
- **Xử lý lỗi:**
  - Sự kiện sai định dạng hoặc thiếu trường đi thẳng vào DLQ, không retry.
  - Lỗi tạm thời (MongoDB, mạng) được thử lại tối đa `NOTIFICATION_EVENTS_MAX_RETRIES` lần (mặc định 4), backoff 1s → 10s, rồi vào DLQ.
  - Loại sự kiện chưa hỗ trợ (ví dụ `MentorRatingUpdated`) được ack và bỏ qua.
- **Log:** chỉ ghi `eventId`, `eventType`, `messageId`, không ghi payload.

### Hợp đồng sự kiện mà notification-service chờ (#73)

auth-service và mentor-service **chưa publish** các sự kiện này (issue #64, #69 của người khác). Khi họ làm, chỉ cần publish lên `mentor.events` theo hợp đồng dưới đây, notification không phải sửa gì.

| eventType | Routing key gợi ý | Trường bắt buộc | Trường tuỳ chọn | Người nhận |
|---|---|---|---|---|
| `AccountRegistered` | `auth.account.registered` | `accountId` (UUID, = `sub` JWT; chấp nhận `userId`) | — | Tài khoản vừa tạo |
| `MentorApproved` | `mentor.verification.approved` | `mentorUserId` (UUID; chấp nhận `userId`) | `mentorId` | Người nộp hồ sơ mentor |
| `MentorRejected` | `mentor.verification.rejected` | `mentorUserId` (UUID; chấp nhận `userId`) | `mentorId`, `reason` (≤ 500 ký tự hiển thị) | Người nộp hồ sơ mentor |

Ví dụ:

```json
{"eventId":"0b9e…","eventType":"MentorRejected","occurredAt":"2026-11-10T10:00:00Z",
 "mentorUserId":"5f1c…","mentorId":17,"reason":"Ảnh CCCD bị mờ"}
```

### Sự kiện booking / payment / review (#81)

Đúng định dạng các service này đang publish trên `develop`; notification chỉ **nhận**, không sửa service nào.

| eventType | Routing key | Người nhận | Cách biết người nhận |
|---|---|---|---|
| `BookingCreated` | `booking.*` | Mentor | ⚠️ booking-service **chưa publish** (mới chỉ ghi log); mapper sẵn sàng |
| `BookingConfirmed` | `booking.lifecycle` | Học viên + Mentor | Tra qua booking (sự kiện chỉ có `bookingId`) |
| `BookingRejected` | `booking.lifecycle` | Học viên (kèm lý do) | Tra qua booking |
| `BookingCancelled` | `booking.lifecycle` | Học viên + Mentor (kèm lý do) | Tra qua booking |
| `BookingConfirmationFailed` | `booking.lifecycle` | Học viên (kèm lý do, nhắc hoàn tiền) | Tra qua booking |
| `BookingCompleted` | `booking.*` | Học viên (mời đánh giá) + Mentor | ⚠️ booking-service **chưa publish**; mapper sẵn sàng |
| `PaymentSucceeded` / `PaymentFailed` / `PaymentRefunded` | `payment.events` | Học viên đã thanh toán | `payerUserId` có sẵn trong sự kiện; số tiền định dạng `500.000 VND` |
| `ReviewCreated` | `review.events` | Mentor được đánh giá | Sự kiện chỉ có `mentorId` (id hồ sơ), nên tra tài khoản mentor qua booking theo `bookingId` |

- **Tra người nhận:** gọi `GET {BOOKING_SERVICE_URL}/internal/v1/bookings/{bookingId}/payment-context` (API có sẵn của booking) để lấy `studentUserId` và `mentorUserId`. Nếu sau này payload mang sẵn `studentUserId` và `mentorUserId` thì dùng luôn, không gọi nữa.
  - Booking không tồn tại (404) hoặc dữ liệu sai: sự kiện đi thẳng vào DLQ.
  - Timeout, 5xx hoặc 429: lỗi tạm thời, retry rồi mới vào DLQ.
  - `bookingId` không phải số: bị từ chối trước khi gọi.
- **Chống trùng:** `eventId` được ghi vào inbox; unique index `(sourceEventId, recipientUserId)` bảo đảm phát lại sự kiện cũ không gửi trùng cho cùng một người.
- **`delivery_attempts`:** mỗi lần đẩy STOMP được ghi lại (`notificationId`, `attempt`, `SENT`/`FAILED`, tên lớp lỗi, thời điểm; tự xoá sau 30 ngày).
  - Đẩy lỗi tạm thời thì thử lại tối đa `NOTIFICATION_DELIVERY_MAX_ATTEMPTS` lần (mặc định 3), backoff nhân đôi từ 200 ms.
  - Hết số lần thử thì thông báo vẫn nằm trong hộp thư.

## 7. Cấu hình (biến môi trường)

| Biến | Bắt buộc | Mặc định | Ý nghĩa |
|---|---|---|---|
| `SERVER_PORT` | | `8089` | Cổng HTTP |
| `MONGODB_URI` | | `mongodb://localhost:27017/mm_notification?replicaSet=rs0` | Chuỗi kết nối; thông tin đăng nhập nằm trong URI |
| `MONGODB_AUTO_INDEX_CREATION` | | `true` | Tự tạo index (gồm unique và TTL ở trên) |
| `AUTH_JWT_ISSUER` | | `auth-service` | Phải khớp auth-service |
| `AUTH_JWT_AUDIENCE` | ✅ | — | Phải khớp `AUTH_JWT_AUDIENCE` của auth-service |
| `AUTH_JWT_PUBLIC_KEY_LOCATION` | ✅ | — | Ví dụ `file:../auth-service/jwt-keys/public.pem` |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` | | `localhost` / `5672` | Cùng tên biến với payment-service |
| `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | | `guest` / `guest` | `guest` chỉ đăng nhập được từ localhost |
| `NOTIFICATION_MESSAGING_ENABLED` | | `true` | `false` để chạy không cần RabbitMQ (không tạo consumer) |
| `NOTIFICATION_EVENTS_EXCHANGE` | | `mentor.events` | Exchange dùng chung |
| `NOTIFICATION_EVENTS_QUEUE` | | `notification.domain-events` | Queue riêng; DLX/DLQ là `<queue>.dlx` / `<queue>.dead` |
| `NOTIFICATION_EVENTS_ROUTING_KEYS` | | `auth.#,mentor.#,booking.#,payment.#,review.#` | Pattern được bind vào queue |
| `BOOKING_SERVICE_URL` | | `http://localhost:8084` | API nội bộ của booking để tra người nhận |
| `BOOKING_CONNECT_TIMEOUT` / `BOOKING_READ_TIMEOUT` | | `2s` / `3s` | Timeout khi gọi booking |
| `NOTIFICATION_DELIVERY_MAX_ATTEMPTS` | | `3` | 1–5 lần đẩy realtime cho mỗi thông báo |
| `NOTIFICATION_DELIVERY_INITIAL_BACKOFF` | | `200ms` | Chờ trước lần thử thứ 2, nhân đôi sau mỗi lần |
| `NOTIFICATION_EVENTS_MAX_RETRIES` | | `4` | 0–10 |
| `NOTIFICATION_CORS_ALLOWED_ORIGINS` | | `http://localhost:5173` | Origin được phép CORS **và** handshake WebSocket |
| `EUREKA_ENABLED` / `EUREKA_URL` | | `false` / `http://localhost:8761/eureka/` | discovery-service có bảo mật: `http://<user>:<password>@<host>:8761/eureka/` |
| `API_DOCS_ENABLED` | | `false` | Bật `/v3/api-docs` và Swagger UI (vẫn cần token) |

Thiếu `AUTH_JWT_AUDIENCE` hoặc `AUTH_JWT_PUBLIC_KEY_LOCATION`, hoặc key không tồn tại hay nhỏ hơn 2048 bit, thì service **dừng ngay khi khởi động**. Thông báo lỗi chỉ ghi tên biến.

Mẫu biến môi trường: [.env.example](.env.example). Không commit file `.env`.

## 8. Chạy local

```bash
# MongoDB replica set
docker run -d --name mm-notification-mongo -p 27017:27017 mongo:8.0 --replSet rs0 --bind_ip_all
docker exec mm-notification-mongo mongosh --quiet --eval 'rs.initiate({_id:"rs0",members:[{_id:0,host:"localhost:27017"}]})'
# RabbitMQ (nếu chưa có broker dùng chung)
docker run -d --name mm-rabbitmq -p 5672:5672 rabbitmq:4.1-alpine

# từ thư mục services/notification-service
export AUTH_JWT_AUDIENCE=<giống auth-service>
export AUTH_JWT_PUBLIC_KEY_LOCATION=file:../auth-service/jwt-keys/public.pem
./mvnw spring-boot:run
```

PowerShell: đặt `$env:AUTH_JWT_AUDIENCE` và `$env:AUTH_JWT_PUBLIC_KEY_LOCATION`, rồi chạy `./mvnw.cmd spring-boot:run`.

Docker (public key được mount lúc chạy, không đóng sẵn vào image):

```bash
docker build -t notification-service .
docker run -p 8089:8089 \
  -v "$PWD/../auth-service/jwt-keys/public.pem:/run/secrets/jwt-public.pem:ro" \
  -e MONGODB_URI="mongodb://host.docker.internal:27017/mm_notification?directConnection=true" \
  -e RABBITMQ_HOST=host.docker.internal -e RABBITMQ_USERNAME=<user> -e RABBITMQ_PASSWORD=<password> \
  -e AUTH_JWT_AUDIENCE=<giống auth-service> \
  -e AUTH_JWT_PUBLIC_KEY_LOCATION=file:/run/secrets/jwt-public.pem \
  notification-service
```

## 9. Bảo mật (tóm tắt)

| Endpoint | Ẩn danh | Có token hợp lệ |
|---|---|---|
| `GET /actuator/health`, `/health/liveness`, `/health/readiness` | 200 (không có chi tiết) | 200 |
| `GET /actuator/info` | 401 | 200 |
| Actuator khác (`env`, `beans`, …) | 401 | 404 (không mở) |
| `/api/v1/notifications/**` | 401 | chỉ thông báo của chính mình |
| `/ws/notifications` | handshake được; **CONNECT bị từ chối** | CONNECT thành công |

- **JWT:** cùng hợp đồng claims với user-service.
- **Stateless:** CSRF tắt vì token chỉ nhận qua header.
- **Lỗi:** dạng `application/problem+json`, có `code` và `requestId`; lỗi 500 không lộ chi tiết; lỗi validation không trả lại giá trị bị từ chối.
- **Eureka:** logger `RedirectingEurekaHttpClient` được hạ xuống WARN để không ghi URL registry có kèm mật khẩu vào log.

## 10. Kiểm thử

```bash
./mvnw clean verify     # cần Docker đang chạy: *IT dùng Testcontainers MongoDB (replica set) + RabbitMQ
./mvnw test             # chỉ unit test, không cần Docker
```

| Loại | Lớp | Số test | Nội dung |
|---|---|---|---|
| Unit | `JwtConfigurationTest` | 15 | Token hợp lệ và 9 kiểu token sai; dừng khởi động khi thiếu cấu hình hoặc key yếu |
| Unit | `ProblemResponsesTest` | 18 | Ánh xạ status → code/type, instance, requestId, `ApiException` |
| Unit | `ApplicationDefaultsTest` | 5 | Cổng, tên, URI Mongo, secret từ biến môi trường, Eureka tắt, actuator tối thiểu, messaging |
| Unit | `DomainEventAndMappersTest` | 16 | Envelope hợp lệ/sai, alias trường, lỗi không lặp lại giá trị, nội dung 3 loại sự kiện |
| Unit | `StompAuthChannelInterceptorTest` | 6 | CONNECT có/không/sai token, header chữ thường, SUBSCRIBE chỉ hàng đợi của mình, chặn SEND |
| Unit | `MessagingPolicyTest` | 4 | Không retry sự kiện sai; retry lỗi tạm thời; giá trị mặc định; giới hạn số lần thử |
| Unit | `NotificationTest` | 4 | UUID id, thời gian mili giây, đánh dấu đọc giữ lần đầu, cắt độ dài, phân loại lỗi transaction |
| IT | `NotificationApiIT` | 7 | Phân trang + sắp xếp, đếm/đánh dấu/đánh dấu tất cả, không đụng được thông báo người khác (404), xoá, id sai, 401 |
| IT | `DomainEventConsumptionIT` | 7 | RabbitMQ thật: tạo thông báo + đẩy STOMP sau commit; gửi lại không trùng; **4 luồng đồng thời → 1 thông báo**; sai định dạng → DLQ không retry; loại lạ bị bỏ qua; routing key chưa bind |
| IT | `StompRealtimeIT` | 7 | CONNECT không/sai token bị từ chối; chỉ người nhận đúng nhận tin; SUBSCRIBE đích lạ hoặc SEND → bị ngắt; origin lạ → 403 |
| IT | `SecurityAndProblemContractIT` | 20 | Hợp đồng lỗi 400/401/403/404/405/500, 7 loại token sai, CORS, actuator, API docs |
| IT | `MongoReplicaSetIT` | 5 | Database `mm_notification`, health Mongo, transaction commit/rollback |
| Unit | `DomainEventMappers81Test` | 7 | Người nhận của từng sự kiện booking đúng #81, payment cho người trả, review cho mentor, định dạng tiền, lý do bị cắt |
| Unit | `BookingParticipantsClientTest` | 6 | Đọc đúng response booking; 404 → lỗi vĩnh viễn; 5xx/429/không kết nối → lỗi tạm thời; chặn `bookingId` không phải số |
| Unit | `RealtimeNotificationPublisherTest` | 5 | Thử lại có giới hạn, ghi từng lần thử, lỗi ghi lịch sử không làm hỏng việc đẩy |
| IT | `BookingPaymentReviewEventsIT` | 7 | RabbitMQ thật + booking-service giả: đúng người nhận và nội dung cho booking/payment/review, phát lại không trùng, `delivery_attempts`, booking 404 → DLQ không retry, booking 503 → retry rồi DLQ |

`ContractProbeController` chỉ có trong `src/test` và không có trong jar.

## 11. Postman

- Collection: [postman/notification-service.postman_collection.json](postman/notification-service.postman_collection.json): 18 request, 30 assertion, gồm thư mục **4. Notification inbox #73**.
- Environment: [postman/notification-local.postman_environment.json](postman/notification-local.postman_environment.json).

Thư mục 4 cần hộp thư của người dùng có ít nhất 1 thông báo, ví dụ sau khi có sự kiện `AccountRegistered`. Nhập `accessToken` do auth-service cấp, rồi chạy:

```bash
npx newman run postman/notification-service.postman_collection.json \
  -e postman/notification-local.postman_environment.json --env-var accessToken=<token>
```

STOMP không kiểm tra được bằng collection runner. Phần này được `StompRealtimeIT` kiểm tra bằng client thật.

## 12. Giới hạn và việc tiếp theo

- **Sự kiện auth/mentor chưa có người publish:** chờ #64 và #69 theo hợp đồng ở [mục 6](#hợp-đồng-sự-kiện-mà-notification-service-chờ-73).
- **Simple broker STOMP nằm trong bộ nhớ:** chỉ phù hợp khi chạy 1 instance. Muốn chạy nhiều instance cần STOMP broker relay hoặc fan-out qua RabbitMQ.
- **Token hết hạn giữa chừng:** phiên WebSocket đã CONNECT vẫn mở sau khi token hết hạn. Client cũ tự kết nối lại khi token mới được cấp.
- **Gateway:** route `/api/v1/notifications/**` và `/ws/notifications/**` thuộc #59 (service của người khác).
- **Cổng 8089, không dùng 8088 như #62 ghi:** trên `develop`, media-service đã dùng `8088` và api-gateway đã route media tới `localhost:8088`. Cổng 8080–8088 đều đã có service. Gateway nên trỏ tới `lb://notification-service` hoặc `8089`.
- **`BookingCreated` và `BookingCompleted`:** booking-service hiện chỉ ghi log, chưa đưa lên RabbitMQ. Đây là việc phía booking. notification đã có sẵn mapper.
- **API nội bộ của booking đang `permitAll`:** notification gọi không cần token. Nếu booking bảo vệ `/internal/**` sau này, cần bổ sung token service-to-service.
- **Lệch kiểu `bookingId` giữa payment (UUID) và booking (Long) trên `develop`:** notification không dùng `bookingId` của payment để tra cứu (người nhận lấy thẳng từ `payerUserId`), nên không bị ảnh hưởng.
