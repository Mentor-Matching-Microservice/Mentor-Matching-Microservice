package com.mentormatching.discovery.support;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Chạy Eureka server thật trên cổng ngẫu nhiên.
 * <p>
 * Credential được sinh ngẫu nhiên cho mỗi lần chạy test, không có giá trị cố định trong repo.
 * Eviction chạy mỗi 500 ms, tắt self-preservation và read-only cache để kiểm tra
 * vòng đời instance trong vài giây, không phải chờ mặc định 60–90 giây.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "eureka.server.enable-self-preservation=false",
        "eureka.server.eviction-interval-timer-in-ms=500",
        "eureka.server.use-read-only-response-cache=false"
})
public abstract class EurekaServerIntegrationTestSupport {

    protected static final String USERNAME = "it-registry-client";
    protected static final String PASSWORD = "it-" + UUID.randomUUID();

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @LocalServerPort
    protected int port;

    @DynamicPropertySource
    static void registryCredentials(DynamicPropertyRegistry registry) {
        registry.add("discovery.security.username", () -> USERNAME);
        registry.add("discovery.security.password", () -> PASSWORD);
    }

    protected String baseUrl() {
        return "http://localhost:" + port;
    }

    /** Service URL đúng định dạng mà các business service khai báo qua EUREKA_URL. */
    protected String serviceUrl(String username, String password) {
        return "http://" + username + ":" + password + "@localhost:" + port + "/eureka/";
    }

    protected static String basic(String username, String password) {
        return "Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    protected HttpResponse<String> send(String method, String path, String authorization, String jsonBody) {
        var builder = HttpRequest.newBuilder(URI.create(baseUrl() + path))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json");
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        var body = jsonBody == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(jsonBody);
        if (jsonBody != null) {
            builder.header("Content-Type", "application/json");
        }
        try {
            return HTTP.send(builder.method(method, body).build(), HttpResponse.BodyHandlers.ofString());
        } catch (java.io.IOException e) {
            throw new IllegalStateException("HTTP call failed: " + method + " " + path, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    protected HttpResponse<String> getAuthenticated(String path) {
        return send("GET", path, basic(USERNAME, PASSWORD), null);
    }

    protected HttpResponse<String> getAnonymous(String path) {
        return send("GET", path, null, null);
    }

    /** Đăng ký instance bằng chính REST API mà Eureka client sử dụng. */
    protected HttpResponse<String> register(String app, String instanceId, int leaseDurationSeconds,
                                            String metadataJson) {
        String payload = """
                {"instance":{
                  "instanceId":"%s","hostName":"localhost","app":"%s","ipAddr":"127.0.0.1",
                  "status":"UP","port":{"$":18080,"@enabled":"true"},
                  "securePort":{"$":443,"@enabled":"false"},"vipAddress":"%s",
                  "dataCenterInfo":{"@class":"com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo","name":"MyOwn"},
                  "leaseInfo":{"renewalIntervalInSecs":1,"durationInSecs":%d},
                  "metadata":%s
                }}""".formatted(instanceId, app, app.toLowerCase(), leaseDurationSeconds, metadataJson);
        return send("POST", "/eureka/apps/" + app, basic(USERNAME, PASSWORD), payload);
    }

    protected HttpResponse<String> heartbeat(String app, String instanceId) {
        return send("PUT", "/eureka/apps/" + app + "/" + instanceId + "?status=UP",
                basic(USERNAME, PASSWORD), null);
    }

    protected HttpResponse<String> getInstance(String app, String instanceId) {
        return getAuthenticated("/eureka/apps/" + app + "/" + instanceId);
    }
}
