# auth-service

Xác thực của Mentor Matching (MySQL `mm_auth`): đăng ký, xác minh email, đăng nhập, xoay vòng refresh token theo họ, đăng xuất, quên/đặt lại mật khẩu và công bố public key JWKS.

- Java 21; Spring Boot 4.1.2; Spring Cloud 2025.1.4; package `com.mentormatching.auth`.
- Cổng mặc định `8081`; MySQL 8.4, database `mm_auth`; schema do Flyway quản lý (`db/migration`).

## 1. API công khai

| Method | Đường dẫn | Mô tả |
|---|---|---|
| POST | `/api/v1/auth/register` | Tạo tài khoản `PENDING_VERIFICATION` (role `LEANER`) và gửi link xác minh email |
| POST | `/api/v1/auth/verify-email` | `{token}` từ link → tài khoản `ACTIVE` |
| POST | `/api/v1/auth/verify-email/resend` | `{email}` → luôn `202`; gửi lại link nếu tài khoản còn chờ xác minh |
| POST | `/api/v1/auth/login` | Trả access token (RS256), đặt cookie `refresh_token` (HttpOnly) |
| POST | `/api/v1/auth/refresh-token` | Xoay vòng refresh token trong cùng họ (`family_id`) |
| POST | `/api/v1/auth/logout` | Thu hồi refresh token hiện tại, xoá cookie |
| POST | `/api/v1/auth/forgot-password` | `{email}` → luôn `202`; gửi link đặt lại mật khẩu nếu tài khoản tồn tại và không bị khoá |
| POST | `/api/v1/auth/reset-password` | `{token, password, confirmPassword}` → đổi mật khẩu, thu hồi mọi refresh token |
| GET | `/.well-known/jwks.json` | Public key ký access token (JWKS, có `kid`), cache 5 phút |

- Các `POST` ở trên không dùng cookie phiên nên bỏ qua CSRF. Bù lại, **chỉ nhận request có header `Origin` thuộc `AUTH_CORS_ALLOWED_ORIGINS`**; request không có Origin hoặc từ origin lạ bị 403.
- Lỗi trả về dạng ProblemDetail, có `code` cố định, ví dụ: `INVALID_CREDENTIALS`, `INVALID_REFRESH_TOKEN`, `INVALID_VERIFICATION_TOKEN`, `INVALID_RESET_TOKEN`, `PASSWORD_MISMATCH`, `EMAIL_ALREADY_EXISTS`.

## 2. Quy tắc bảo mật (#63)

- **Access token:**
  - ký RS256 bằng khóa RSA ≥ 2048 bit;
  - claim gồm `sub` (UUID accountId), `roles`, `iss`, `aud`, `exp`;
  - header có `kid` = thumbprint RFC 7638 của public key.
- **JWKS (`/.well-known/jwks.json`):**
  - chỉ có phần công khai (`kty, e, n, kid, use=sig, alg=RS256`); private key không bao giờ rời service;
  - resource service có thể dùng `NimbusJwtDecoder.withJwkSetUri(...)` thay cho file `public.pem`.
- **Refresh token:**
  - 32 byte ngẫu nhiên; DB chỉ lưu hash SHA-256;
  - gửi qua cookie `HttpOnly`, có `SameSite`, path `/api/v1/auth`;
  - mỗi lần refresh tạo token mới cùng họ và thu hồi token cũ;
  - **nếu một token đã xoay hoặc đã thu hồi được gửi lại**, có thể nó đã bị đánh cắp, nên service **thu hồi toàn bộ họ token**. Việc thu hồi được commit dù request trả 401. Phiên đăng nhập khác (họ khác) không bị ảnh hưởng.
- **Trạng thái tài khoản:** `PENDING_VERIFICATION`, `LOCKED` và `DISABLED` đều không đăng nhập hay refresh được.
- **Link xác minh email và đặt lại mật khẩu:**
  - token 32 byte, chỉ lưu hash;
  - **dùng một lần** và có hạn (24 giờ / 30 phút); dùng xong thì các link khác cùng loại của tài khoản cũng bị vô hiệu;
  - mỗi tài khoản chỉ được gửi tối đa 1 email trong `AUTH_EMAIL_RESEND_COOLDOWN`;
  - `forgot-password` và `verify-email/resend` trả lời giống hệt nhau dù email có tồn tại hay không.
- **Đặt lại mật khẩu thành công:**
  - thu hồi **mọi** refresh token của tài khoản (đăng xuất mọi thiết bị);
  - tài khoản còn chờ xác minh được kích hoạt, vì mở được link trong email đã chứng minh sở hữu email;
  - tài khoản `LOCKED`/`DISABLED` không đặt lại được.
- **Email:**
  - chỉ gửi **sau khi transaction commit**; lỗi gửi chỉ ghi log;
  - khi `AUTH_MAIL_ENABLED=false` (mặc định), service **không gửi email** và không ghi token ra log;
  - chỉ khi phát triển local và đặt `AUTH_MAIL_LOG_LINKS=true` thì link mới được ghi ra log; không thể bật đồng thời với SMTP thật.

## 3. Endpoint nội bộ

- `POST /internal/accounts/statuses` và `PUT /internal/accounts/{accountId}/mentor-role`, dùng cho mentor-service.
- Chỉ bật khi `AUTH_INTERNAL_ACCOUNT_SYNC_ENABLED=true`; cần `Authorization: Bearer <AUTH_INTERNAL_ACCOUNT_SYNC_TOKEN>` (≥ 32 ký tự).
- Gán role `MENTOR` là **idempotent**: gọi lại không lỗi và không tạo bản ghi trùng.

## 4. Cấu hình

Xem [.env.example](.env.example). Bắt buộc: `DB_PASSWORD`, `AUTH_JWT_AUDIENCE`, `AUTH_JWT_PRIVATE_KEY_PATH` (PKCS#8 PEM, RSA ≥ 2048 bit). Biến mới của #63:

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `AUTH_EMAIL_VERIFICATION_TTL` | `24h` | Hạn link xác minh email (≤ 7 ngày) |
| `AUTH_PASSWORD_RESET_TTL` | `30m` | Hạn link đặt lại mật khẩu (≤ 24 giờ) |
| `AUTH_EMAIL_RESEND_COOLDOWN` | `60s` | Chờ tối thiểu giữa hai email cho cùng tài khoản |
| `AUTH_MAIL_ENABLED` | `false` | `true` để gửi qua SMTP (`SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`) |
| `AUTH_MAIL_FROM` | `no-reply@mentor-matching.local` | Người gửi |
| `AUTH_VERIFY_EMAIL_URL` / `AUTH_RESET_PASSWORD_URL` | `http://localhost:5173/verify-email` / `/reset-password` | Trang frontend nhận `?token=` |
| `AUTH_MAIL_LOG_LINKS` | `false` | Chỉ cho local: ghi link ra log khi không gửi email |

Role `LEANER`, `MENTOR`, `ADMIN` được Flyway `V2__seed_core_roles.sql` tạo nếu chưa có. `db/seed/accounts.sql` vẫn là dữ liệu mẫu tuỳ chọn.

## 5. Build và test

```bash
./mvnw clean verify    # cần Docker: integration test dùng Testcontainers MySQL 8.4
./mvnw test            # chỉ unit test
```

| Lớp | Số test | Nội dung |
|---|---|---|
| `AccountTokenAndMailConfigTest` | 3 | Token một lần ngẫu nhiên, chỉ lưu hash; giới hạn TTL; link email; không cho bật log link cùng SMTP |
| `AuthFlowsIT` | 12 | JWKS chỉ có public key và kiểm tra được chữ ký và `kid`; đăng ký → xác minh → đăng nhập; token xác minh dùng lại, hết hạn hoặc giả bị từ chối; resend/forgot không lộ email; reset đổi mật khẩu, thu hồi phiên cũ, link dùng một lần, link cũ hoặc hết hạn bị từ chối; POST thiếu hoặc sai Origin bị 403; dùng lại refresh token cũ thu hồi cả họ; tài khoản DISABLED không đăng nhập hay refresh được; gán role MENTOR idempotent |

## 6. Chạy local

```bash
export DB_PASSWORD=...  AUTH_JWT_AUDIENCE=mentor-matching-api  AUTH_JWT_PRIVATE_KEY_PATH=$PWD/jwt-keys/private.pem
./mvnw spring-boot:run
```

`jwt-keys/private.pem` chỉ là khóa mẫu cho môi trường dev. Môi trường thật phải dùng khóa riêng và không commit.
