# notification-service

Service quản lý **hộp thư thông báo** của người dùng (MongoDB `mm_notification`) và **đẩy thông báo realtime** qua STOMP WebSocket.

## 1. Trạng thái

| Phạm vi | Issue | Trạng thái |
|---|---|---|
| Khởi tạo project, MongoDB replica set + transaction, STOMP broker, Eureka client, RS256 resource server, ProblemDetail, Testcontainers MongoDB | #62 (phần notification) | ✅ Đã làm (nhánh này) |
| REST API hộp thư, xác thực STOMP ở frame CONNECT, consumer `AccountRegistered` / `MentorApproved` / `MentorRejected`, inbox dedup | #73 | ⏳ Chưa làm |
| Event Booking / Payment / Review, `delivery_attempts`, retry, dedup theo `eventId + recipientUserId` | #81 | ⏳ Chưa làm |

Phần payment-service / review-service (MSSQL) của #62 **không** nằm trong nhánh này.

## 2. Thông tin chính

| Mục | Giá trị |
|---|---|
| Cổng | `8088` (`SERVER_PORT`) |
| Database | MongoDB **replica set**, database `mm_notification` |
| Stack | Java 21, Spring Boot 4.1.2, Spring Cloud 2025.1.4 |
| Xác thực | Bearer JWT **RS256** do auth-service ký; chỉ cần public key |
| Realtime | STOMP qua WebSocket `/ws/notifications`, đích người dùng `/user/queue/notifications` |
| Discovery | Eureka client, tắt mặc định (`EUREKA_ENABLED=false`) |

## 3. Cấu trúc

```
src/main/java/com/mentormatching/notification
├── NotificationServiceApplication.java
├── config
│   ├── JwtConfiguration.java        # JwtDecoder RS256 + kiểm tra claims (iss, aud, exp, sub=UUID, roles)
│   ├── SecurityConfiguration.java   # Bearer JWT, stateless, ProblemDetail cho 401/403
│   ├── CorsConfig.java              # CORS khi gọi thẳng service (gateway lo CORS tập trung)
│   ├── MongoConfiguration.java      # MongoTransactionManager (cần replica set)
│   └── WebSocketConfiguration.java  # STOMP endpoint + simple broker
├── exception                        # Hợp đồng lỗi dùng chung với user-service
│   ├── ApiException.java
│   ├── GlobalExceptionHandler.java
│   ├── ProblemErrorController.java
│   ├── ProblemResponses.java
│   └── RequestIdFilter.java
└── security
    ├── ProblemAuthenticationEntryPoint.java
    └── ProblemAccessDeniedHandler.java
```

## 4. Cấu hình (biến môi trường)

| Biến | Bắt buộc | Mặc định | Ý nghĩa |
|---|---|---|---|
| `SERVER_PORT` | | `8088` | Cổng HTTP |
| `MONGODB_URI` | | `mongodb://localhost:27017/mm_notification?replicaSet=rs0` | Chuỗi kết nối, thông tin đăng nhập nằm trong URI |
| `MONGODB_AUTO_INDEX_CREATION` | | `true` | Tự tạo index từ `@Indexed` |
| `AUTH_JWT_ISSUER` | | `auth-service` | Phải khớp auth-service |
| `AUTH_JWT_AUDIENCE` | ✅ | — | Phải khớp `AUTH_JWT_AUDIENCE` của auth-service |
| `AUTH_JWT_PUBLIC_KEY_LOCATION` | ✅ | — | Ví dụ `file:../auth-service/jwt-keys/public.pem` |
| `NOTIFICATION_CORS_ALLOWED_ORIGINS` | | `http://localhost:5173` | Origin được phép CORS **và** handshake WebSocket |
| `EUREKA_ENABLED` | | `false` | Bật đăng ký vào discovery-service |
| `EUREKA_URL` | | `http://localhost:8761/eureka/` | Với discovery-service có bảo mật: `http://<user>:<password>@<host>:8761/eureka/` |
| `API_DOCS_ENABLED` | | `false` | Bật `/v3/api-docs` và Swagger UI (vẫn cần token) |

Thiếu `AUTH_JWT_AUDIENCE` hoặc `AUTH_JWT_PUBLIC_KEY_LOCATION`, hoặc key không tồn tại hay nhỏ hơn 2048 bit, thì service **dừng ngay khi khởi động**. Thông báo lỗi chỉ ghi tên biến, không in giá trị.

Mẫu biến môi trường: [.env.example](.env.example). Không commit file `.env`.

## 5. Chạy local

Cần MongoDB chạy dạng replica set. Ví dụ dùng Docker:

```bash
docker run -d --name mm-notification-mongo -p 27017:27017 mongo:8.0 --replSet rs0 --bind_ip_all
docker exec mm-notification-mongo mongosh --quiet --eval 'rs.initiate({_id:"rs0",members:[{_id:0,host:"localhost:27017"}]})'
```

Chạy service (bash), từ thư mục `services/notification-service`:

```bash
export AUTH_JWT_AUDIENCE=<giống auth-service>
export AUTH_JWT_PUBLIC_KEY_LOCATION=file:../auth-service/jwt-keys/public.pem
./mvnw spring-boot:run
```

PowerShell:

```powershell
$env:AUTH_JWT_AUDIENCE = "<giống auth-service>"
$env:AUTH_JWT_PUBLIC_KEY_LOCATION = "file:../auth-service/jwt-keys/public.pem"
./mvnw.cmd spring-boot:run
```

Docker (public key được mount lúc chạy, không đóng sẵn vào image):

```bash
docker build -t notification-service .
docker run -p 8088:8088 \
  -v "$PWD/../auth-service/jwt-keys/public.pem:/run/secrets/jwt-public.pem:ro" \
  -e MONGODB_URI="mongodb://host.docker.internal:27017/mm_notification?directConnection=true" \
  -e AUTH_JWT_AUDIENCE=<giống auth-service> \
  -e AUTH_JWT_PUBLIC_KEY_LOCATION=file:/run/secrets/jwt-public.pem \
  notification-service
```

## 6. Bảo mật

| Endpoint | Ẩn danh | Có token hợp lệ |
|---|---|---|
| `GET /actuator/health`, `/health/liveness`, `/health/readiness` | 200 (không có chi tiết) | 200 |
| `GET /actuator/info` | 401 | 200 |
| Actuator khác (`env`, `beans`, …) | 401 | 404 (không mở) |
| `/ws/notifications` (handshake) | 401 | 101 Switching Protocols |
| Mọi đường dẫn khác | 401 | theo controller |

- **JWT** có cùng hợp đồng với user-service:
  - chữ ký RS256 bằng public key;
  - `iss` và `aud` đúng cấu hình;
  - có `exp`;
  - `sub` là UUID, tức accountId;
  - `roles` là danh sách chuỗi, được chuyển thành `ROLE_<role>`.
- **Stateless:** không session, không cookie. CSRF tắt vì token chỉ đi qua header `Authorization`.
- **WebSocket:**
  - Handshake hiện dùng chung filter Bearer JWT. Principal của phiên chính là userId, nên `convertAndSendToUser(userId, …)` gửi đúng người.
  - Handshake từ trình duyệt chỉ được chấp nhận từ origin trong `NOTIFICATION_CORS_ALLOWED_ORIGINS`, để chống Cross-Site WebSocket Hijacking.
  - #73 sẽ chuyển xác thực sang frame STOMP CONNECT, vì trình duyệt không gửi được header khi handshake.
- **Lỗi:**
  - Mọi lỗi trả về `application/problem+json`, gồm `code` cố định, `requestId` (trùng header `X-Request-ID`) và `instance`.
  - Lỗi 500 không lộ thông điệp hay stack trace.
  - Lỗi validation chỉ trả tên field, không trả lại giá trị bị từ chối.
- **Eureka client:** logger `RedirectingEurekaHttpClient` được hạ xuống WARN, để URL registry có kèm mật khẩu không bị ghi vào log.

## 7. Gửi thông báo realtime (cho #73 / #81)

```java
messagingTemplate.convertAndSendToUser(recipientUserId.toString(),
        WebSocketConfiguration.NOTIFICATION_QUEUE, payload);
```

Client STOMP đăng ký `/user/queue/notifications`.

Khi cần ghi bản ghi dedup (`inbox`) và thông báo nguyên tử, dùng `@Transactional`. `MongoTransactionManager` đã được cấu hình sẵn.

## 8. Kiểm thử

```bash
./mvnw clean verify     # cần Docker đang chạy cho các test *IT (Testcontainers)
./mvnw test             # chỉ unit test, không cần Docker
```

| Loại | Lớp | Số test | Nội dung |
|---|---|---|---|
| Unit | `JwtConfigurationTest` | 15 | Token hợp lệ; sai aud/iss, hết hạn, thiếu exp, sub không phải UUID, roles sai, ký bằng key khác; dừng khởi động khi thiếu cấu hình hoặc key yếu |
| Unit | `ProblemResponsesTest` | 18 | Ánh xạ status → code/type, instance, requestId, ghi response, `ApiException` |
| Unit | `ApplicationDefaultsTest` | 4 | Cổng, tên service, URI Mongo, secret lấy từ biến môi trường, Eureka tắt, actuator tối thiểu |
| IT | `SecurityAndProblemContractIT` | 20 | 401/403/404/405/400/500 dạng ProblemDetail, 7 loại token sai, role → method security, CORS, actuator, API docs |
| IT | `MongoReplicaSetIT` | 5 | Đúng database `mm_notification`, health Mongo UP, transaction commit/rollback trên replica set |
| IT | `RealtimeInfrastructureIT` | 5 | STOMP client thật: chỉ người nhận đúng nhận được tin, handshake thiếu/sai token → 401, origin lạ → 403 |

`ContractProbeController` chỉ có trong `src/test`, dùng để kiểm tra hợp đồng lỗi khi service chưa có API nghiệp vụ. Nó không có trong jar.

## 9. Postman

- Collection: [postman/notification-service.postman_collection.json](postman/notification-service.postman_collection.json)
- Environment: [postman/notification-local.postman_environment.json](postman/notification-local.postman_environment.json)

Nhập `accessToken` là access token do auth-service cấp (`POST /api/v1/auth/login`), rồi chạy:

```bash
npx newman run postman/notification-service.postman_collection.json \
  -e postman/notification-local.postman_environment.json --env-var accessToken=<token>
```

## 10. Giới hạn và việc tiếp theo

- **Chưa có API nghiệp vụ hay consumer sự kiện:** làm ở #73 và #81.
- **Simple broker nằm trong bộ nhớ:** chỉ phù hợp khi chạy 1 instance. Muốn chạy nhiều instance cần STOMP broker relay (RabbitMQ) hoặc fan-out qua message broker (#57).
- **Gateway:** route `/api/v1/notifications/**` và `/ws/notifications/**` sẽ được thêm ở #59.
- **Docker Compose:** MongoDB replica set dùng chung sẽ được dựng ở #57.
- **Cổng 8088 đang trùng với media-service:** các nhánh media hiện để mặc định `8088`, trong khi issue #61 ghi media dùng `8087`. Cần thống nhất khi dựng Docker Compose (#57).
