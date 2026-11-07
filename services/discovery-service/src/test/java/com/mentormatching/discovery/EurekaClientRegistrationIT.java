package com.mentormatching.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.jayway.jsonpath.JsonPath;
import com.mentormatching.discovery.support.EurekaServerIntegrationTestSupport;
import com.mentormatching.testclient.SampleEurekaClientApplication;
import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.http.RestClientDiscoveryClientOptionalArgs;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Một Spring Boot client thật (spring-cloud-starter-netflix-eureka-client, cùng BOM với các business service)
 * đăng ký vào registry với credential nhúng trong service URL, đúng cách business service cấu hình EUREKA_URL.
 */
@ExtendWith(OutputCaptureExtension.class)
class EurekaClientRegistrationIT extends EurekaServerIntegrationTestSupport {

    private static final String APP = "IT-SAMPLE-CLIENT";
    private static final Duration TIMEOUT = Duration.ofSeconds(45);

    /**
     * Cấu hình khuyến nghị cho mọi business service: eureka-client 2.0.6 log nguyên service URL
     * (kèm credential) ở mức INFO khi không gọi được registry. Xem README, mục bảo mật.
     */
    static final String RECOMMENDED_CLIENT_LOGGING =
            "--logging.level.com.netflix.discovery.shared.transport.decorator.RedirectingEurekaHttpClient=WARN";

    @Test
    void clientRegistersIsDiscoverableAndDeregistersOnShutdown(CapturedOutput output) {
        String instanceId = "it-sample-client:1";
        ConfigurableApplicationContext client = startClient(instanceId, serviceUrl(USERNAME, PASSWORD), true);
        try {
            // Cùng transport với business service (không có Jersey): RestClient + Basic auth lấy từ URL.
            assertThat(client.getBean(AbstractDiscoveryClientOptionalArgs.class))
                    .isInstanceOf(RestClientDiscoveryClientOptionalArgs.class);

            // Registry nhận đăng ký kèm metadata.
            await().atMost(TIMEOUT).pollInterval(Duration.ofMillis(500))
                    .until(() -> getInstance(APP, instanceId).statusCode() == 200);
            String instance = getInstance(APP, instanceId).body();
            assertThat(JsonPath.<String>read(instance, "$.instance.status")).isEqualTo("UP");
            assertThat(JsonPath.<String>read(instance, "$.instance.metadata.version")).isEqualTo("1.2.3");
            assertThat(JsonPath.<String>read(instance, "$.instance.vipAddress")).isEqualTo("it-sample-client");

            // Tra cứu service qua DiscoveryClient theo tên logic (spring.application.name).
            DiscoveryClient discovery = client.getBean(DiscoveryClient.class);
            await().atMost(TIMEOUT).pollInterval(Duration.ofMillis(500))
                    .until(() -> !discovery.getInstances("it-sample-client").isEmpty());
            List<ServiceInstance> instances = discovery.getInstances("it-sample-client");
            assertThat(instances).hasSize(1);
            assertThat(instances.getFirst().getInstanceId()).isEqualTo(instanceId);
            assertThat(instances.getFirst().getHost()).isEqualTo("localhost");
            assertThat(instances.getFirst().getPort()).isEqualTo(18082);
            assertThat(instances.getFirst().getMetadata()).containsEntry("version", "1.2.3");
            assertThat(discovery.getServices()).contains("it-sample-client");

            // Heartbeat định kỳ của client làm mới lease.
            long firstRenewal = lastRenewal(instanceId);
            await().atMost(TIMEOUT).pollInterval(Duration.ofMillis(500))
                    .until(() -> lastRenewal(instanceId) > firstRenewal);
        } finally {
            client.close();
        }

        // Tắt client: instance được hủy đăng ký ngay, không phải chờ lease hết hạn.
        await().atMost(TIMEOUT).pollInterval(Duration.ofMillis(250))
                .until(() -> getAuthenticated("/eureka/apps/" + APP).statusCode() == 404);

        // Credential không xuất hiện trong log của registry lẫn client.
        assertThat(output.getAll()).doesNotContain(PASSWORD);
    }

    @Test
    void clientWithWrongCredentialsIsRejectedWithPlain401AndNothingLeaks(CapturedOutput output) {
        String instanceId = "it-sample-client:wrong-credentials";
        String wrongPassword = PASSWORD + "-wrong";
        // Cố ý KHÔNG áp logging khuyến nghị: registry trả 401 không body nên client không đi vào nhánh
        // exception (nhánh log nguyên service URL).
        ConfigurableApplicationContext client = startClient(instanceId, serviceUrl(USERNAME, wrongPassword), false);
        try {
            // Client thử lại mỗi giây trong 6 giây, tất cả bị từ chối.
            await().during(Duration.ofSeconds(6)).atMost(Duration.ofSeconds(8))
                    .pollInterval(Duration.ofMillis(500))
                    .until(() -> getInstance(APP, instanceId).statusCode() == 404);
        } finally {
            client.close();
        }
        assertThat(output.getAll())
                .contains("Request execution failure with status code 401")
                .doesNotContain(wrongPassword);
    }

    @Test
    void unreachableRegistryDoesNotLeakCredentialsWithRecommendedClientLogging(CapturedOutput output)
            throws Exception {
        String secret = "it-unreachable-" + PASSWORD;
        String unreachableUrl = "http://" + USERNAME + ":" + secret + "@localhost:" + freePort() + "/eureka/";
        ConfigurableApplicationContext client = startClient("it-sample-client:unreachable", unreachableUrl, true);
        try {
            // Để client thử đăng ký, heartbeat và fetch registry vài lần với registry không tồn tại.
            await().atMost(TIMEOUT).pollInterval(Duration.ofMillis(500))
                    .until(() -> output.getAll().contains("was unable to refresh its cache"));
        } finally {
            client.close();
        }
        assertThat(output.getAll()).doesNotContain(secret);
    }

    private long lastRenewal(String instanceId) {
        return JsonPath.<Long>read(getInstance(APP, instanceId).body(), "$.instance.leaseInfo.lastRenewalTimestamp");
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static ConfigurableApplicationContext startClient(String instanceId, String serviceUrl,
                                                              boolean recommendedLogging) {
        List<String> args = new ArrayList<>(List.of(
                // Không nạp application.yml của registry.
                "--spring.config.name=it-sample-client",
                "--spring.main.banner-mode=off",
                "--spring.application.name=it-sample-client",
                "--eureka.client.service-url.defaultZone=" + serviceUrl,
                // Business service không có Jersey nên dùng RestClient; ép test client dùng đúng transport đó.
                "--eureka.client.jersey.enabled=false",
                "--eureka.client.register-with-eureka=true",
                "--eureka.client.fetch-registry=true",
                "--eureka.client.registry-fetch-interval-seconds=1",
                "--eureka.client.initial-instance-info-replication-interval-seconds=1",
                "--eureka.client.instance-info-replication-interval-seconds=1",
                "--eureka.instance.instance-id=" + instanceId,
                "--eureka.instance.hostname=localhost",
                "--eureka.instance.non-secure-port=18082",
                "--eureka.instance.lease-renewal-interval-in-seconds=1",
                "--eureka.instance.lease-expiration-duration-in-seconds=5",
                "--eureka.instance.metadata-map.version=1.2.3"));
        if (recommendedLogging) {
            args.add(RECOMMENDED_CLIENT_LOGGING);
        }
        return new SpringApplicationBuilder(SampleEurekaClientApplication.class)
                .web(WebApplicationType.NONE)
                .run(args.toArray(String[]::new));
    }
}
