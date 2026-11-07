# booking-service

Nền tảng Booking Service độc lập, dùng SQL Server và Flyway. Đây là bước khởi tạo service theo ticket #61; nghiệp vụ booking sẽ được triển khai tiếp theo domain đã có ở modular monolith cũ.

- Java 21; Spring Boot 4.1.2; Spring Cloud 2025.1.4; Maven Wrapper riêng.
- Package `com.mentormatching.booking`; port mặc định `8084`.
- DBMS **Microsoft SQL Server**, database riêng `mm_booking` và user runtime riêng.
- Flyway tạo bảng `bookings` và `mentor_availabilities`; booking `PENDING` có thời hạn giữ chỗ để Payment xác minh trước khi tạo Checkout.
- Internal endpoint `GET /internal/v1/bookings/{bookingId}/payment-context` chỉ trả dữ liệu thanh toán cho đúng learner, và chỉ khi booking còn `PENDING` trong thời hạn giữ chỗ.
- JDBC driver và Flyway database module theo SQL Server.
- Dependency chung và phiên bản: [service initialization](../../docs/service-initialization.md).

## Build

Từ thư mục service:

```bash
./mvnw clean verify
```

`clean verify` chạy integration test trên SQL Server bằng Testcontainers; Docker cần chạy. H2 không được dùng thay SQL Server vì migration và kiểu dữ liệu phụ thuộc MSSQL.

## Chạy sau khi chuẩn bị database

Tạo database/user riêng; đặt `DB_PASSWORD` qua shell hoặc IDE. `DB_URL`, `DB_USERNAME`, `SERVER_PORT` có giá trị mặc định trong application.yml. `.env.example` là mẫu tham khảo, Spring Boot không tự nạp `.env`.

```bash
./mvnw spring-boot:run
```

Flyway bật, JPA chỉ validate. Startup cần SQL Server và credentials hợp lệ. Eureka client bật mặc định và đăng ký tới `EUREKA_URL`; tắt bằng `EUREKA_ENABLED=false` khi chạy độc lập. Swagger mặc định tắt. Actuator expose health/info. JWT issuer/audience/public key cấu hình bằng `AUTH_JWT_*`.

## Scheduling

PostgreSQL range/exclusion constraint và btree_gist sẽ được triển khai bằng migration sau khi chốt schema/state giữ lịch. Chưa tạo extension, bảng hoặc constraint trong lượt init này.
