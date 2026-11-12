package com.mentormatching.notification.support;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Chạy toàn bộ ứng dụng trên cổng ngẫu nhiên với MongoDB thật (Testcontainers, single-node
 * replica set như môi trường dev), RabbitMQ thật và public key JWT sinh riêng cho lần chạy.
 * Container được khởi động một lần và dùng chung cho mọi lớp *IT; cần Docker đang chạy.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class NotificationIntegrationTestSupport {

    public static final String DATABASE = "mm_notification";
    public static final String ALLOWED_ORIGIN = "http://localhost:5173";

    // Testcontainers 2.x không còn mặc định chạy replica set: bật tường minh để có transaction.
    protected static final MongoDBContainer MONGO = new MongoDBContainer("mongo:8.0").withReplicaSet();
    protected static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:4.1-alpine");
    protected static final TestJwtKeys KEYS = TestJwtKeys.trusted();
    protected static final JsonMapper JSON = JsonMapper.builder().build();
    /** booking-service giả cho việc tra người nhận (#81); dùng chung cho mọi lớp *IT. */
    protected static final FakeBookingService BOOKINGS = new FakeBookingService();

    static {
        MONGO.start();
        RABBIT.start();
    }

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    protected int port;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // Đi qua đúng khoá spring.mongodb.uri mà application.yml dùng (không dùng @ServiceConnection).
        registry.add("spring.mongodb.uri", () -> MONGO.getReplicaSetUrl(DATABASE));
        registry.add("notification.jwt.public-key-location", KEYS::publicKeyLocation);
        registry.add("notification.jwt.issuer", () -> TestJwtKeys.ISSUER);
        registry.add("notification.jwt.audience", () -> TestJwtKeys.AUDIENCE);
        registry.add("notification.cors.allowed-origins", () -> ALLOWED_ORIGIN);
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
        registry.add("notification.booking.base-url", BOOKINGS::baseUrl);
        // Ít lần thử để test lỗi tạm thời chạy nhanh; giá trị thật mặc định là 4.
        registry.add("notification.messaging.max-retries", () -> "1");
        registry.add("notification.delivery.initial-backoff", () -> "10ms");
    }

    protected HttpResponse<String> get(String path, String bearerToken) {
        return send(request(path, bearerToken).GET().build());
    }

    protected HttpResponse<String> get(String path) {
        return get(path, null);
    }

    protected HttpResponse<String> send(String method, String path, String bearerToken,
                                        Map<String, String> headers, String jsonBody) {
        HttpRequest.Builder builder = request(path, bearerToken);
        headers.forEach(builder::header);
        HttpRequest.BodyPublisher body = jsonBody == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(jsonBody);
        if (jsonBody != null) {
            builder.header("Content-Type", "application/json");
        }
        return send(builder.method(method, body).build());
    }

    protected static JsonNode json(HttpResponse<String> response) {
        return JSON.readTree(response.body());
    }

    private HttpRequest.Builder request(String path, String bearerToken) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(10));
        if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        return builder;
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }
}
