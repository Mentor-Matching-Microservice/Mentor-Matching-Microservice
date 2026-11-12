package com.mentormatching.auth.support;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Toàn bộ auth-service trên cổng ngẫu nhiên với MySQL 8.4 và RabbitMQ thật (Testcontainers, Flyway chạy đủ migration),
 * khóa RSA sinh riêng cho lần chạy và email bị "bắt lại" trong bộ nhớ. Cần Docker đang chạy.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(AuthIntegrationTestSupport.MailCapture.class)
public abstract class AuthIntegrationTestSupport {

    public static final String ORIGIN = "http://localhost:5173";
    public static final String AUDIENCE = "mentor-matching-api";
    public static final String INTERNAL_TOKEN = "it-internal-" + UUID.randomUUID();
    public static final String PASSWORD = "Str0ng-Passw0rd!";
    protected static final JsonMapper JSON = JsonMapper.builder().build();

    protected static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4").withDatabaseName("mm_auth");
    protected static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:4.1-alpine");
    private static final Path PRIVATE_KEY = writePrivateKey();

    static {
        MYSQL.start();
        RABBIT.start();
    }

    @TestConfiguration
    static class MailCapture {
        @Bean
        @Primary
        CapturingMailSender capturingMailSender() {
            return new CapturingMailSender();
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("auth.jwt.audience", () -> AUDIENCE);
        registry.add("auth.jwt.private-key-path", PRIVATE_KEY::toString);
        registry.add("auth.cors.allowed-origins", () -> ORIGIN);
        registry.add("auth.tokens.resend-cooldown", () -> "0s");
        registry.add("auth.internal.account-sync.enabled", () -> "true");
        registry.add("auth.internal.account-sync.token", () -> INTERNAL_TOKEN);
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
        // Gửi outbox nhanh và ít lần thử để test chạy nhanh; giá trị thật mặc định 2s / 4 lần.
        registry.add("auth.messaging.publish-delay", () -> "300ms");
        registry.add("auth.messaging.publish-initial-delay", () -> "300ms");
        registry.add("auth.messaging.confirm-timeout", () -> "2s");
        registry.add("auth.messaging.max-retries", () -> "1");
    }

    @LocalServerPort
    protected int port;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    protected HttpResponse<String> post(String path, String json) {
        return post(path, json, null);
    }

    /** POST như trình duyệt từ frontend (có Origin); {@code cookie} là giá trị refresh_token nếu có. */
    protected HttpResponse<String> post(String path, String json, String refreshCookie) {
        HttpRequest.Builder builder = request(path).header("Origin", ORIGIN).header("Content-Type", "application/json");
        if (refreshCookie != null) {
            builder.header("Cookie", "refresh_token=" + refreshCookie);
        }
        return send(builder.POST(HttpRequest.BodyPublishers.ofString(json == null ? "" : json)).build());
    }

    protected HttpResponse<String> get(String path) {
        return send(request(path).GET().build());
    }

    protected HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl() + path)).timeout(Duration.ofSeconds(15));
    }

    protected String baseUrl() {
        return "http://localhost:" + port;
    }

    protected HttpResponse<String> send(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    protected static JsonNode json(HttpResponse<String> response) {
        return JSON.readTree(response.body());
    }

    /** Giá trị cookie refresh_token trong Set-Cookie (null nếu không có). */
    protected static String refreshCookie(HttpResponse<String> response) {
        return response.headers().allValues("Set-Cookie").stream()
                .filter(c -> c.startsWith("refresh_token="))
                .map(c -> c.substring("refresh_token=".length(), c.indexOf(';')))
                .filter(v -> !v.isEmpty())
                .findFirst().orElse(null);
    }

    protected static String registerBody(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"confirmPassword\":\"" + password + "\"}";
    }

    protected static String loginBody(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    protected static String uniqueEmail() {
        return "it-" + UUID.randomUUID().toString().substring(0, 12) + "@example.com";
    }

    private static Path writePrivateKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            byte[] pkcs8 = generator.generateKeyPair().getPrivate().getEncoded();
            String pem = "-----BEGIN PRIVATE KEY-----\n"
                    + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(pkcs8)
                    + "\n-----END PRIVATE KEY-----\n";
            Path file = Files.createTempFile("auth-it-private", ".pem");
            file.toFile().deleteOnExit();
            Files.writeString(file, pem, StandardCharsets.US_ASCII);
            return file;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
