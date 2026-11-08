# payment-service

Payment service implements Stripe Checkout, payment lookup, verified webhook state changes, admin refunds, idempotency and scheduled reconciliation.

- Java 21; Spring Boot 4.1.2; Spring Cloud 2025.1.4; Maven Wrapper riêng.
- Package `com.mentormatching.payment`; port mặc định `8085`.
- DBMS **PostgreSQL**, database riêng `mm_payment` và user runtime riêng.
- JDBC driver, Flyway module và Testcontainers theo đúng PostgreSQL.
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

Flyway bật nhưng thư mục migration chưa có SQL; JPA chỉ validate, không tự tạo schema. Startup cần database và credentials hợp lệ. Eureka mặc định tắt đến khi có Discovery. Swagger mặc định tắt; bật API_DOCS_ENABLED khi cần và cấu hình quyền truy cập sau. Actuator expose health/info, ẩn health details. Spring Security vẫn auto-configuration mặc định; chưa có JWT decoder/issuer configuration của dự án.

## API and Stripe setup

- `POST /api/v1/payments` requires `Idempotency-Key` and `{ "bookingId": "UUID" }`. The service reads amount, currency, learner and mentor from Booking Service; callers cannot submit a price.
- `GET /api/v1/payments/me` and `GET /api/v1/payments/{id}` return the caller's payment record.
- `POST /api/v1/payments/stripe/webhook` verifies `Stripe-Signature`; Stripe CLI can forward local events to this endpoint.
- `POST /api/v1/payments/{id}/refund` is admin-only and requires an idempotency key. An omitted amount requests the remaining balance.

Booking Service must expose `GET /api/v1/bookings/{id}/payment-summary` returning bookingId, learnerId, mentorId, status, totalAmount and currency. Checkout is only allowed for a `PENDING` booking owned by the current learner. Stripe webhook event IDs and refund keys are persisted to prevent duplicate effects. A scheduled job reconciles expired checkout sessions with Stripe.

Configure `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `AUTH_JWT_*`, and `BOOKING_SERVICE_URL`; sample values are in `.env.example`. Do not commit live Stripe secrets.
