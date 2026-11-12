package com.mentormatching.user.support;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Toàn bộ user-service trên cổng ngẫu nhiên với MySQL 8.4 và RabbitMQ thật (Testcontainers) và khóa JWT sinh riêng.
 * Cần Docker đang chạy.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class UserIntegrationTestSupport {

    public static final String EXCHANGE = "mentor.events";
    protected static final JsonMapper JSON = JsonMapper.builder().build();
    protected static final TestJwtKeys KEYS = TestJwtKeys.trusted();
    /** auth-service giả cho PATCH trạng thái (#66). */
    protected static final FakeAuthService AUTH = new FakeAuthService();

    protected static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4").withDatabaseName("mm_user");
    protected static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:4.1-alpine");

    static {
        MYSQL.start();
        RABBIT.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
        registry.add("user.jwt.public-key-location", KEYS::publicKeyLocation);
        registry.add("user.jwt.issuer", () -> TestJwtKeys.ISSUER);
        registry.add("user.jwt.audience", () -> TestJwtKeys.AUDIENCE);
        registry.add("user.messaging.max-retries", () -> "1");
        registry.add("user.auth.base-url", AUTH::baseUrl);
        registry.add("user.auth.timeout", () -> "3s");
    }

    @LocalServerPort
    protected int port;
    @Autowired
    protected RabbitTemplate rabbit;
    @Autowired
    protected RabbitAdmin rabbitAdmin;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    protected HttpResponse<String> get(String path, String bearer) {
        HttpRequest.Builder b = request(path);
        if (bearer != null) {
            b.header("Authorization", "Bearer " + bearer);
        }
        return send(b.GET().build());
    }

    protected HttpResponse<String> send(String method, String path, String bearer, String json) {
        HttpRequest.Builder b = request(path).header("Content-Type", "application/json")
                .method(method, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json));
        if (bearer != null) {
            b.header("Authorization", "Bearer " + bearer);
        }
        return send(b.build());
    }

    protected HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(Duration.ofSeconds(15));
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

    /** Publish đúng định dạng auth-service phát (outbox #64): {eventId, eventType, occurredAt, ...}. */
    protected String publish(String routingKey, String eventType, Map<String, Object> fields) {
        String eventId = UUID.randomUUID().toString();
        publishWithId(routingKey, eventId, eventType, fields);
        return eventId;
    }

    protected void publishWithId(String routingKey, String eventId, String eventType, Map<String, Object> fields) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventId", eventId);
        body.put("eventType", eventType);
        body.put("occurredAt", "2026-11-12T03:00:00Z");
        body.putAll(fields);
        publishRaw(routingKey, JSON.writeValueAsBytes(body));
    }

    protected void publishRaw(String routingKey, byte[] body) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType("application/json");
        rabbit.send(EXCHANGE, routingKey, new Message(body, properties));
    }

    protected long queueDepth(String queue) {
        var info = rabbitAdmin.getQueueInfo(queue);
        return info == null ? -1 : info.getMessageCount();
    }

    protected static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
