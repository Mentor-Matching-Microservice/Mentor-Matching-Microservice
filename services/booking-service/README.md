# booking-service

Nền tảng Booking Service độc lập, dùng SQL Server và Flyway. Đây là bước khởi tạo service theo ticket #61; nghiệp vụ booking sẽ được triển khai tiếp theo domain đã có ở modular monolith cũ.

- Java 21; Spring Boot 4.1.2; Spring Cloud 2025.1.4; Maven Wrapper riêng.
- Package `com.mentormatching.booking`; port mặc định `8084`.
- DBMS **Microsoft SQL Server**, database riêng `mm_booking` và user runtime riêng.
- JDBC driver và Flyway database module theo SQL Server.
- Dependency chung và phiên bản: [service initialization](../../docs/service-initialization.md).

## Build

Từ thư mục service:

```bash
./mvnw clean verify
```

Build kiểm tra dependency resolution, compile và executable JAR; hiện chưa có test case. Chưa cần database/Docker cho build này. Integration tests sau phải dùng PostgreSQL thực, không thay bằng H2.

## Chạy sau khi chuẩn bị database

Tạo database/user riêng; đặt `DB_PASSWORD` qua shell hoặc IDE. `DB_URL`, `DB_USERNAME`, `SERVER_PORT` có giá trị mặc định trong application.yml. `.env.example` là mẫu tham khảo, Spring Boot không tự nạp `.env`.

```bash
./mvnw spring-boot:run
```

Flyway bật, JPA chỉ validate. Startup cần SQL Server và credentials hợp lệ. Eureka mặc định tắt; Swagger mặc định tắt. Actuator expose health/info. JWT issuer/audience/public key cấu hình bằng `AUTH_JWT_*`.

## Scheduling

PostgreSQL range/exclusion constraint và btree_gist sẽ được triển khai bằng migration sau khi chốt schema/state giữ lịch. Chưa tạo extension, bảng hoặc constraint trong lượt init này.
