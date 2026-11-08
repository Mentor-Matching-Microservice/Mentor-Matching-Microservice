# discovery-service

Eureka service registry của Mentor Matching (issue #58). Các service đăng ký instance của mình vào đây và tra cứu địa chỉ của nhau theo tên logic (`spring.application.name`) thay vì hard-code host/port.

| Trạng thái | Nội dung |
|---|---|
| ✅ **Đã triển khai** | Eureka Server độc lập (standalone), bảo vệ bằng HTTP Basic, health/readiness probe công khai, dashboard tắt mặc định, cấu hình qua biến môi trường, Dockerfile, unit test + integration test (gồm một Eureka client thật), Postman collection |
| ✅ **Đã kiểm chứng đăng ký client** | 6 service đang có Eureka client (auth, user, mentor, booking, payment, review) đăng ký và hiện `UP` khi bật bằng biến môi trường. Không sửa code của các service này. Xem [Kiểm chứng đăng ký các service hiện có](#kiểm-chứng-đăng-ký-các-service-hiện-có-tiêu-chí-58) |
| ⏳ **Việc của issue khác** | Gateway chưa dùng `lb://` (#59); catalog-service chưa có Eureka client và sẽ gộp vào mentor-service (#60); chưa có Docker Compose (#57). Xem [Việc tích hợp còn lại](#12-giới-hạn-đã-biết-và-việc-tích-hợp-còn-lại) |

---

## 1. Mục đích

- Lưu **registry**: service nào đang chạy, ở host/port nào, trạng thái gì (`UP`, `DOWN`, `OUT_OF_SERVICE`...), kèm metadata.
- Nhận **đăng ký** và **heartbeat** (gia hạn lease) từ Eureka client trong mỗi service.
- Loại bỏ (evict) instance ngừng gửi heartbeat, để client không bị điều hướng tới instance đã chết.
- Cho phép client **tra cứu** instance theo tên service; Gateway và OpenFeign/LoadBalancer sau này dùng để gọi `lb://user-service` thay vì `http://localhost:8082`.

## 2. Trách nhiệm kiến trúc

```text
           ┌──────────────────────── discovery-service :8761 ────────────────────────┐
           │  Eureka registry (in-memory)   ·   HTTP Basic   ·   /actuator/health     │
           └──────────────▲───────────────────────────▲──────────────────────────────┘
      đăng ký + heartbeat │ (Basic, mỗi 30 s)          │ fetch registry (mỗi 30 s)
     ┌────────────┬───────┴────┬────────────┐   ┌──────┴────────────┐
     │auth-service│user-service│mentor-svc …│   │api-gateway (lb://)│  ← đích đến, chưa làm
     └────────────┴────────────┴────────────┘   └───────────────────┘
```

| Sở hữu | Không sở hữu |
|---|---|
| Registry instance, lease, heartbeat, trạng thái instance | Nghiệp vụ, database, entity/repository của service khác |
| Kiểm soát truy cập vào registry | Định tuyến request nghiệp vụ (việc của Gateway) |
| Health probe của chính nó | Phân quyền người dùng cuối (JWT của auth-service không dùng ở đây) |

Registry chạy **standalone**: không tự đăng ký, không fetch registry, không replicate sang peer. Toàn bộ registry nằm trong bộ nhớ và được dựng lại từ heartbeat sau khi khởi động lại; không cần database.

## 3. Cấu trúc project

```text
services/discovery-service/
├── pom.xml                         # Boot 4.1.2 + Cloud 2025.1.4, giống các service khác
├── Dockerfile, .dockerignore       # multi-stage, chạy bằng user không phải root
├── .env.example                    # chỉ có placeholder
├── postman/                        # collection + environment kiểm chứng
└── src/
    ├── main/java/com/mentormatching/discovery/
    │   ├── DiscoveryServiceApplication.java          # @EnableEurekaServer
    │   ├── config/DiscoverySecurityProperties.java   # validate DISCOVERY_USERNAME/PASSWORD
    │   ├── config/DiscoveryCredentialsConfiguration.java  # tài khoản nội bộ, BCrypt in-memory
    │   ├── config/SecurityConfiguration.java         # filter chain: chỉ health là public
    │   └── security/ProblemAuthenticationEntryPoint.java  # 401 (ProblemDetail / body rỗng cho /eureka/**)
    ├── main/resources/application.yml
    └── test/java/
        ├── com/mentormatching/discovery/...           # unit test (*Test) và integration test (*IT)
        └── com/mentormatching/testclient/...          # Eureka client tối giản chỉ dùng trong test
```

Không có tầng controller/service/repository vì registry không có nghiệp vụ (đúng quy ước "Gateway và Discovery không ép CQRS").

## 4. Dependencies

| Dependency | Phiên bản (do BOM quyết định) | Mục đích |
|---|---|---|
| `spring-cloud-starter-netflix-eureka-server` | Spring Cloud Netflix 5.0.3, eureka-core 2.0.6 | Eureka Server |
| `spring-boot-starter-security` | Boot 4.1.2 | HTTP Basic cho registry |
| `spring-boot-starter-actuator` | Boot 4.1.2 | health / liveness / readiness |
| `spring-boot-configuration-processor` | Boot 4.1.2 | metadata cho `discovery.security.*` |
| Test: `spring-boot-starter-webmvc-test`, `spring-boot-starter-security-test`, `awaitility` | Boot BOM | unit + integration test |

Không ghi đè version nào ngoài Boot parent `4.1.2` và `spring-cloud.version` `2025.1.4`, đúng như các service hiện có.

## 5. Cấu hình

Tất cả đọc từ biến môi trường. Spring Boot **không** tự nạp `.env`; `.env.example` chỉ để tham khảo.

| Biến môi trường | Mặc định | Ý nghĩa |
|---|---|---|
| `DISCOVERY_USERNAME` | *(bắt buộc, không có mặc định)* | Tài khoản nội bộ. Chỉ gồm chữ, số, `.`, `_`, `-` (không có `:` vì HTTP Basic tách tại dấu `:` đầu tiên) |
| `DISCOVERY_PASSWORD` | *(bắt buộc, không có mặc định)* | 16–72 byte (giới hạn của BCrypt). Từ chối giá trị placeholder `replace-with…` |
| `SERVER_PORT` | `8761` | Cổng HTTP (cổng mà mọi service đang mặc định trỏ tới) |
| `EUREKA_INSTANCE_HOSTNAME` | `localhost` | Hostname của registry. Trong Docker đặt bằng tên service/container |
| `EUREKA_DASHBOARD_ENABLED` | `false` | Bật dashboard tại `/` (vẫn yêu cầu đăng nhập) |
| `EUREKA_SELF_PRESERVATION` | `true` | Chế độ tự bảo vệ: khi số heartbeat tụt mạnh, Eureka ngừng evict để tránh xóa nhầm lúc mạng chập chờn. Local nên đặt `false` để instance chết biến mất nhanh |
| `EUREKA_RENEWAL_PERCENT_THRESHOLD` | `0.85` | Ngưỡng kích hoạt self-preservation |
| `EUREKA_EVICTION_INTERVAL_MS` | `60000` | Chu kỳ quét và loại instance hết lease |
| `EUREKA_RESPONSE_CACHE_UPDATE_INTERVAL_MS` | `30000` | Chu kỳ làm mới cache danh sách app. Thay đổi có thể trễ tối đa chừng này khi đọc `/eureka/apps` |

Các giá trị cố định trong `application.yml`:
- `eureka.client.register-with-eureka=false`, `fetch-registry=false`: registry standalone.
- `eureka.client.service-url.defaultZone` trỏ về chính nó: không bao giờ coi một host/port khác là peer (đã kiểm chứng bằng test: danh sách peer rỗng).
- `wait-time-in-ms-when-sync-empty=0`: không có peer nên phục vụ ngay sau khi khởi động.
- Actuator chỉ expose `health,info`; `show-details: never`.

Thiếu hoặc sai `DISCOVERY_USERNAME`/`DISCOVERY_PASSWORD` thì ứng dụng **dừng ngay khi khởi động**, với thông báo nêu tên biến nhưng **không in lại giá trị**:

```text
Failed to bind properties under 'discovery.security' ...
    Reason: java.lang.IllegalArgumentException: discovery.security.username must be set (environment variable DISCOVERY_USERNAME)
```

## 6. Chạy local

Yêu cầu: JDK 21. Không cần database hay Docker.

```bash
cd services/discovery-service
./mvnw clean verify                     # build + toàn bộ test

# bash
export DISCOVERY_USERNAME=discovery
export DISCOVERY_PASSWORD="$(openssl rand -base64 33 | tr '+/' '-_')"
export EUREKA_DASHBOARD_ENABLED=true EUREKA_SELF_PRESERVATION=false
./mvnw spring-boot:run
```

```powershell
# PowerShell
$env:DISCOVERY_USERNAME = "discovery"
$env:DISCOVERY_PASSWORD = -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 32 | ForEach-Object { [char]$_ })
$env:EUREKA_DASHBOARD_ENABLED = "true"; $env:EUREKA_SELF_PRESERVATION = "false"
./mvnw spring-boot:run
```

Kiểm tra nhanh:

```bash
curl http://localhost:8761/actuator/health                                    # {"status":"UP",...}
curl -u "discovery:$DISCOVERY_PASSWORD" -H 'Accept: application/json' http://localhost:8761/eureka/apps
# Dashboard: mở http://localhost:8761/ rồi đăng nhập bằng tài khoản trên (nếu EUREKA_DASHBOARD_ENABLED=true)
```

Docker (image tag cố định `eclipse-temurin:21.0.10_7-*-noble`, chạy bằng user `app`):

```bash
docker build -t mentor-matching/discovery-service services/discovery-service
docker run --rm -p 8761:8761 -e DISCOVERY_USERNAME=discovery -e DISCOVERY_PASSWORD=... mentor-matching/discovery-service
```

> Dockerfile chưa được build thử vì máy phát triển không có Docker daemon khi thực hiện #58. Sẽ kiểm chứng cùng Docker Compose ở #57.

## 7. Đăng ký một service vào registry

Các service `auth`, `user`, `mentor`, `booking`, `payment`, `review` **đã có sẵn** dependency `spring-cloud-starter-netflix-eureka-client` và cấu hình:

```yaml
eureka:
  client:
    enabled: ${EUREKA_ENABLED:false}
    service-url:
      defaultZone: ${EUREKA_URL:http://localhost:8761/eureka/}
```

Vì vậy để đăng ký, **không cần sửa code**, chỉ cần đặt biến môi trường khi chạy service:

```bash
EUREKA_ENABLED=true
EUREKA_URL=http://discovery:<DISCOVERY_PASSWORD>@localhost:8761/eureka/
```

- Credential nằm trong `userinfo` của URL. Eureka client (transport RestClient) tách ra thành header `Authorization: Basic …` và **bỏ** khỏi URL thực gọi.
- Ký tự đặc biệt trong mật khẩu phải được percent-encode trong URL (`@` → `%40`, `/` → `%2F`...). Dùng mật khẩu chỉ gồm `[A-Za-z0-9-_]` để khỏi phải encode.
- Thiếu credential hoặc sai mật khẩu thì registry trả `401`, client log `Request execution failure with status code 401` và **không** được đăng ký.
- **Bắt buộc thêm** cấu hình log sau vào service khi bật Eureka (lý do ở [mục 9](#9-bảo-mật)):

```yaml
logging:
  level:
    com.netflix.discovery.shared.transport.decorator.RedirectingEurekaHttpClient: WARN
```

Tên app trong registry là `spring.application.name` viết hoa: `AUTH-SERVICE`, `USER-SERVICE`, `MENTOR-SERVICE`, `BOOKING-SERVICE`, `PAYMENT-SERVICE`, `REVIEW-SERVICE`, và sau này `NOTIFICATION-SERVICE`, `MEDIA-SERVICE`, `API-GATEWAY-SERVICE`.

## 8. Tra cứu service (discovery)

Sau khi đăng ký với `fetch-registry=true` (mặc định của client), service tra cứu instance bằng tên logic:

```java
List<ServiceInstance> instances = discoveryClient.getInstances("user-service");
```

Hoặc gián tiếp qua load balancer (đều dựa trên registry này):
- Spring Cloud Gateway: `uri: lb://user-service`
- OpenFeign: `@FeignClient(name = "user-service")`
- RestClient: `@LoadBalanced RestClient.Builder` rồi gọi `http://user-service/...`

Phía registry (đã kiểm chứng bằng test): instance hết lease hoặc đã hủy đăng ký không còn được trả về, và instance bị đặt `OUT_OF_SERVICE` được báo đúng trạng thái. Phía client, Eureka client mặc định chỉ dùng instance `UP` (`eureka.client.filter-only-up-instances=true`). Đây là hành vi mặc định của thư viện, chưa có test riêng trong #58.

## 9. Bảo mật

Registry là thành phần **nội bộ**: không publish port 8761 ra Internet, không route qua API Gateway.

| Đường dẫn | Không đăng nhập | Tài khoản nội bộ |
|---|---|---|
| `GET /actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness` | 200 (chỉ `status`, không có chi tiết) | 200 |
| `/eureka/**` (REST API: đăng ký, heartbeat, tra cứu) | 401, **body rỗng** + `WWW-Authenticate: Basic` | 200/204 |
| `/` (dashboard) | 401 | 404 khi tắt (mặc định), 200 khi bật |
| `/actuator/info` | 401 (ProblemDetail) | 200 |
| `/actuator/env`, `beans`, `configprops`, `heapdump`, `threaddump`, `loggers`, `mappings`, `metrics` | 401 | **404**, không expose |
| Mọi đường dẫn khác | 401 trước khi định tuyến (không dò được endpoint nào tồn tại) | 404 |

Các quyết định và lý do:
- **HTTP Basic + một tài khoản nội bộ**: đây là cơ chế Eureka client hỗ trợ sẵn qua service URL, không cần sửa code business service. JWT của auth-service dành cho người dùng cuối, không dùng cho service-to-registry.
- **Không có credential mặc định**: thiếu biến môi trường thì không khởi động. Mật khẩu chỉ được giữ dạng BCrypt trong bộ nhớ; `toString()` của cấu hình che mật khẩu; lỗi validate không in giá trị. Không còn "Using generated security password" trong log.
- **Stateless**: không tạo session, không set cookie. **CSRF tắt** vì Eureka client gửi `POST/PUT/DELETE` không có token và xác thực bằng header, không bằng cookie. Request ghi từ trình duyệt khác origin là non-simple (JSON/PUT/DELETE) nên bị chặn ở preflight, vì registry không bật CORS.
- **401 body rỗng cho `/eureka/**`**: nếu trả JSON ProblemDetail, Eureka client sẽ cố đọc body thành registry, lỗi parse và đi vào nhánh log có lộ URL (xem dưới). Các đường dẫn khác vẫn trả ProblemDetail như các service còn lại.
- **Phát hiện khi kiểm thử, lộ credential ở phía client**: `RedirectingEurekaHttpClient` của eureka-client 2.0.6 log **nguyên service URL kèm mật khẩu** ở mức `INFO` mỗi khi request lỗi (ví dụ registry chưa khởi động). Đây là hành vi của thư viện client, không phải của registry. Kết quả đo bằng test:

  | Tình huống | Không có cấu hình log | Có `RedirectingEurekaHttpClient: WARN` |
  |---|---|---|
  | Sai mật khẩu | 0 dòng lộ (nhờ 401 body rỗng) | 0 |
  | Registry không truy cập được | **13 dòng lộ** trong ~4 giây | **0** |

  Do đó mọi service bật Eureka **phải** thêm cấu hình log ở [mục 7](#7-đăng-ký-một-service-vào-registry). Test `EurekaClientRegistrationIT` giữ hai kết quả này làm regression test.
- **Dashboard**: tắt mặc định; khi bật vẫn yêu cầu đăng nhập. Ẩn đường dẫn không được coi là biện pháp bảo mật.
- **Giới hạn**: HTTP Basic gửi credential dạng base64 trên mỗi request, nên chỉ an toàn trong mạng nội bộ (Docker network) hoặc qua TLS. Chưa có TLS và chưa có rotation tự động (xem mục 12).

## 10. Kiểm thử

```bash
cd services/discovery-service
./mvnw test          # chỉ unit test (*Test, Surefire)
./mvnw verify        # unit test + integration test (*IT, Failsafe)
```

Test không cần database, Docker hay credential thật. Integration test chạy registry thật trên cổng ngẫu nhiên với **mật khẩu sinh ngẫu nhiên mỗi lần chạy**.

| Lớp test | Loại | Nội dung |
|---|---|---|
| `DiscoverySecurityPropertiesTest` | unit | Validate username/password: thiếu, rỗng, ký tự cấm, quá ngắn, vượt 72 byte, placeholder; lỗi không lặp lại giá trị; `toString` che mật khẩu |
| `DiscoveryCredentialsConfigurationTest` | unit (`ApplicationContextRunner`) | Context fail khi thiếu biến; mật khẩu lưu dạng `{bcrypt}`; đúng role; lỗi không lộ giá trị |
| `ProblemAuthenticationEntryPointTest` | unit | 401 + challenge; ProblemDetail cho đường dẫn thường, body rỗng cho `/eureka/**`; tôn trọng context path |
| `DefaultConfigurationIT` | integration | Map biến môi trường; standalone, không có peer; giá trị mặc định an toàn cho production; dashboard bật vẫn cần đăng nhập |
| `DiscoverySecurityIT` | integration | Health public không có chi tiết; 401 cho ẩn danh, sai mật khẩu, user lạ; không set cookie; ẩn danh không đăng ký được; 8 endpoint actuator nhạy cảm trả 404; dashboard tắt mặc định |
| `RegistryLifecycleIT` | integration | Đăng ký kèm metadata, tra cứu theo app và VIP; heartbeat giữ instance vượt quá thời hạn lease; ngừng heartbeat thì bị evict; instance không heartbeat bị ẩn khi hết hạn, bị evict rồi heartbeat trả 404; hủy đăng ký xóa ngay; `OUT_OF_SERVICE` override và gỡ override |
| `EurekaClientRegistrationIT` | integration | **Eureka client thật**, cùng transport RestClient với business service, credential trong URL: đăng ký, `DiscoveryClient` tìm thấy kèm metadata, heartbeat gia hạn lease, tắt client thì hủy đăng ký; sai credential bị 401 và không đăng ký; registry không truy cập được không lộ mật khẩu khi có cấu hình log khuyến nghị |

Kết quả lần chạy khi hoàn thành #58 (Windows, JDK 21.0.10): `./mvnw clean verify` cho **24 unit test + 37 integration test, 0 lỗi**. Integration test được chạy thêm 2 lần nữa để kiểm tra độ ổn định, cả hai lần 37/37 pass.

### Kiểm chứng đăng ký các service hiện có (tiêu chí #58)

Script [verification/verify-client-registration.sh](verification/verify-client-registration.sh) chạy thật discovery-service cùng 6 service đang có Eureka client, **không sửa source** của các service này. Client được bật hoàn toàn bằng biến môi trường có sẵn (`EUREKA_ENABLED`, `EUREKA_URL`).

Script làm các bước sau:
1. Dựng MySQL 8.4 cho auth, user, mentor và PostgreSQL 16 cho booking, payment, review (đúng như trên `develop`) bằng Docker. Mỗi service có database và user riêng.
2. Build discovery-service và 6 service từ source hiện tại. Bỏ qua test của các service đó, vì script chỉ kiểm tra lúc chạy.
3. Chạy discovery-service, rồi chạy 6 service với `EUREKA_ENABLED=true` và `EUREKA_URL=http://discovery:<mật khẩu>@localhost:8761/eureka/`, kèm cấu hình log `RedirectingEurekaHttpClient=WARN` ở [mục 7](#7-đăng-ký-một-service-vào-registry).
4. Chờ tới khi cả 6 app có trạng thái `UP` trong `/eureka/apps`, rồi gọi `/actuator/health` của từng service.
5. Kiểm tra mọi mật khẩu sinh trong lần chạy không xuất hiện trong log của bất kỳ service nào.
6. Dừng toàn bộ process và xóa container.

```bash
# Cần Docker đang chạy, Java 21, curl; cổng 8761 và 8081-8086 phải trống.
bash services/discovery-service/verification/verify-client-registration.sh
```

Kết quả ngày 2026-11-08 (Windows, Docker 29.1.3, chạy 2 lần, cả hai PASS, mỗi lần khoảng 3 phút gồm cả build):

```
SERVICE            INSTANCE                             EUREKA   HEALTH
auth-service       localhost:auth-service:8081          UP       200
user-service       localhost:user-service:8082          UP       200
mentor-service     localhost:mentor-service:8083        UP       200
booking-service    localhost:booking-service:8084       UP       200
payment-service    localhost:payment-service:8085       UP       200
review-service     localhost:review-service:8086        UP       200

[verify] occurrences of the generated passwords in all service logs: 0
[verify] PASS: all six existing services registered with discovery-service and are UP
```

api-gateway-service và catalog-service **không** nằm trong danh sách vì chúng chưa có dependency Eureka client trên `develop`. Việc thêm thuộc #59 (gateway chuyển sang `lb://`) và #60 (catalog gộp vào mentor-service). Xem [mục 12](#12-giới-hạn-đã-biết-và-việc-tích-hợp-còn-lại).

## 11. Postman

File trong `postman/`:
- `discovery-service.postman_collection.json`: 21 request, 31 assertion, chia 4 thư mục: probe công khai, kiểm tra bị từ chối, vòng đời instance mẫu, bề mặt quản trị.
- `discovery-local.postman_environment.json`: `baseUrl`, `discoveryUsername`, `discoveryPassword` (kiểu *secret*, **để trống**), `dashboardEnabled`, `sampleApp`, `sampleInstanceId`.

Postman app: *Import* cả hai file, chọn environment `discovery-local`, nhập `discoveryPassword` vào **Current value** (không lưu vào Initial value / không export), rồi *Run collection* theo đúng thứ tự.

Dòng lệnh (Newman), không ghi mật khẩu vào file:

```bash
npx newman@6.2.1 run postman/discovery-service.postman_collection.json \
  -e postman/discovery-local.postman_environment.json \
  --env-var "discoveryPassword=$DISCOVERY_PASSWORD" \
  --env-var "dashboardEnabled=$EUREKA_DASHBOARD_ENABLED"
```

Lưu ý:
- Cần truy cập mạng nội bộ tới registry (máy local hoặc Docker network). Không mở registry ra ngoài chỉ để chạy Postman.
- Thư mục 03 đăng ký instance giả `POSTMAN-SAMPLE-SERVICE` rồi hủy ở cuối. Không chạy trên registry dùng chung hay production.
- Các assertion dùng endpoint một instance (`/eureka/apps/{app}/{id}`), không bị cache, nên kết quả không phụ thuộc `EUREKA_RESPONSE_CACHE_UPDATE_INTERVAL_MS`.

## 12. Giới hạn đã biết và việc tích hợp còn lại

**Giới hạn**
- Standalone, một node: registry khởi động lại thì mất registry trong bộ nhớ cho đến khi các client gửi heartbeat/đăng ký lại (tối đa khoảng 30 giây). Chưa cấu hình cụm peer.
- Một tài khoản dùng chung cho mọi service và cho người vận hành; chưa có TLS; đổi mật khẩu phải khởi động lại registry và cập nhật `EUREKA_URL` ở mọi service.
- Đọc `/eureka/apps` có thể trễ tối đa `EUREKA_RESPONSE_CACHE_UPDATE_INTERVAL_MS` (mặc định 30 giây). Client mặc định fetch mỗi 30 giây, nên một service mới có thể mất tới khoảng 1 phút mới được service khác thấy.
- Eviction mặc định: lease 90 giây + chu kỳ quét 60 giây, và self-preservation có thể giữ instance chết lâu hơn. Local nên đặt `EUREKA_SELF_PRESERVATION=false`.
- Dockerfile đã được build và chạy thử ngày 2026-11-07:
  - thiếu credentials thì container dừng;
  - health trả 200; `/eureka/apps` trả 401 khi ẩn danh, 200 khi đúng mật khẩu;
  - process chạy bằng user `app`, không phải root;
  - mật khẩu không xuất hiện trong log.

**Việc tích hợp còn lại (không làm trong #58)**

| Thành phần | Thay đổi cần làm | Thuộc |
|---|---|---|
| auth, user, mentor, booking, payment, review | **Đã kiểm chứng đăng ký `UP`** bằng script ở [mục 10](#kiểm-chứng-đăng-ký-các-service-hiện-có-tiêu-chí-58), không cần sửa code. Khi đưa vào Compose chỉ cần đặt `EUREKA_ENABLED=true`, `EUREKA_URL` có credential, `LOGGING_LEVEL_COM_NETFLIX_DISCOVERY_SHARED_TRANSPORT_DECORATOR_REDIRECTINGEUREKAHTTPCLIENT=WARN`, và trong Docker thêm `EUREKA_INSTANCE_PREFER_IP_ADDRESS=true` hoặc hostname phù hợp | #57 (Compose) |
| api-gateway-service | Thêm `spring-cloud-starter-netflix-eureka-client`, đổi `uri: http://localhost:808x` sang `lb://<service>` | #59 |
| catalog-service | Chưa có Spring Cloud BOM và eureka-client; theo #60 sẽ được gộp vào mentor-service nên không nên tích hợp riêng | #60 |
| notification-service, media-service | Tạo project kèm eureka-client và cấu hình như trên | #62, #61 |
| Docker Compose | Service `discovery-service` (healthcheck `/actuator/health/readiness`), truyền `DISCOVERY_PASSWORD` qua secret/env, các service phụ thuộc `condition: service_healthy` | #57 |
