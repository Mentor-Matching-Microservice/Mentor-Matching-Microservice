package com.mentormatching.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import com.netflix.eureka.cluster.PeerEurekaNodes;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.server.EurekaServerConfigBean;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Chạy với cấu hình mặc định trong application.yml, chỉ cung cấp các biến môi trường
 * như khi triển khai thật (DISCOVERY_USERNAME, DISCOVERY_PASSWORD, EUREKA_DASHBOARD_ENABLED).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DefaultConfigurationIT {

    static final String USERNAME = "it-default-config";
    /** Sinh ngẫu nhiên mỗi lần chạy, không lưu credential cố định trong repo. */
    static final String PASSWORD = "it-" + UUID.randomUUID();

    @DynamicPropertySource
    static void environmentVariables(DynamicPropertyRegistry registry) {
        // Đặt đúng tên biến môi trường mà application.yml tham chiếu.
        registry.add("DISCOVERY_USERNAME", () -> USERNAME);
        registry.add("DISCOVERY_PASSWORD", () -> PASSWORD);
        registry.add("EUREKA_DASHBOARD_ENABLED", () -> "true");
    }

    @LocalServerPort
    int port;

    @Autowired
    Environment environment;

    @Autowired
    EurekaClientConfigBean clientConfig;

    @Autowired
    EurekaServerConfigBean serverConfig;

    @Autowired
    PeerEurekaNodes peerEurekaNodes;

    @Test
    void environmentVariablesAreMappedToRegistryCredentials() {
        assertThat(environment.getProperty("discovery.security.username")).isEqualTo(USERNAME);
        assertThat(environment.getProperty("spring.application.name")).isEqualTo("discovery-service");
    }

    @Test
    void runsAsStandaloneRegistryWithoutPeers() {
        assertThat(clientConfig.shouldRegisterWithEureka()).isFalse();
        assertThat(clientConfig.shouldFetchRegistry()).isFalse();
        // Không replicate sang server khác (ví dụ một registry đang chạy ở cổng 8761).
        assertThat(peerEurekaNodes.getPeerEurekaNodes()).isEmpty();
        assertThat(serverConfig.getWaitTimeInMsWhenSyncEmpty()).isZero();
    }

    @Test
    void usesProductionSafeLeaseDefaults() {
        assertThat(serverConfig.shouldEnableSelfPreservation()).isTrue();
        assertThat(serverConfig.getRenewalPercentThreshold()).isEqualTo(0.85);
        assertThat(serverConfig.getEvictionIntervalTimerInMs()).isEqualTo(60_000L);
        assertThat(serverConfig.getResponseCacheUpdateIntervalMs()).isEqualTo(30_000L);
    }

    @Test
    void enabledDashboardStillRequiresAuthentication() throws Exception {
        HttpClient http = HttpClient.newHttpClient();
        HttpRequest anonymous = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/")).build();
        HttpRequest authenticated = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/"))
                .header("Authorization", "Basic " + Base64.getEncoder()
                        .encodeToString((USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8)))
                .build();

        assertThat(http.send(anonymous, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
        HttpResponse<String> dashboard = http.send(authenticated, HttpResponse.BodyHandlers.ofString());
        assertThat(dashboard.statusCode()).isEqualTo(200);
        assertThat(dashboard.body()).contains("Instances currently registered with Eureka");
    }
}
