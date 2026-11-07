# payment-service

Payment checkout branch implements Stripe Checkout, payment lookup and checkout idempotency.

- Java 21; Spring Boot 4.1.2; Spring Cloud 2025.1.4; Maven Wrapper riêng.
- Package `com.mentormatching.payment`; port mặc định `8085`.
- DBMS **Microsoft SQL Server**, database riêng `mm_payment` và user runtime riêng.
- JDBC driver, Flyway module và Testcontainers theo đúng SQL Server.
- Dependency chung và phiên bản: [service initialization](../../docs/service-initialization.md).

## Build

Từ thư mục service:

```bash
./mvnw clean verify
```

`clean verify` chạy integration tests trên SQL Server bằng Testcontainers; Docker cần chạy. Không thay SQL Server bằng H2 vì Flyway migration dùng cú pháp và kiểu dữ liệu MSSQL.

## Chạy sau khi chuẩn bị database

Tạo database/user riêng; đặt `DB_PASSWORD` qua shell hoặc IDE. `DB_URL`, `DB_USERNAME`, `SERVER_PORT` có giá trị mặc định trong application.yml. `.env.example` là mẫu tham khảo, Spring Boot không tự nạp `.env`.

```bash
./mvnw spring-boot:run
```

Flyway bật, JPA chỉ validate. Startup cần SQL Server và credentials hợp lệ. Eureka mặc định tắt đến khi có Discovery. Swagger mặc định tắt; bật API_DOCS_ENABLED khi cần. Actuator expose health/info, ẩn health details. JWT decoder kiểm tra issuer, audience và RSA public key.

## API and Stripe setup

- `POST /api/v1/payments` requires `Idempotency-Key` and `{ "bookingId": "UUID" }`. The service reads amount, currency, learner and mentor from Booking Service; callers cannot submit a price.
- `GET /api/v1/payments/me`, `GET /api/v1/payments/mentor/me`, and `GET /api/v1/payments/{id}` return only records the caller is allowed to view.
Payment calls `GET /internal/v1/bookings/{bookingId}/payment-context` with the current user's bearer token. Booking validates ownership, `PENDING` status, and an unexpired hold, then returns the authoritative amount and currency. The client cannot submit the amount. Requests require an `Idempotency-Key`.

Configure `STRIPE_SECRET_KEY`, `AUTH_JWT_*`, and `BOOKING_SERVICE_URL`; sample values are in `.env.example`. Do not commit live Stripe secrets.
