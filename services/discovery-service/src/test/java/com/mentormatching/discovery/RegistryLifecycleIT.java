package com.mentormatching.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.jayway.jsonpath.JsonPath;
import com.mentormatching.discovery.support.EurekaServerIntegrationTestSupport;

import org.junit.jupiter.api.Test;

/**
 * Vòng đời instance qua REST API của Eureka (cùng giao thức mà Eureka client dùng):
 * đăng ký, tra cứu, heartbeat, đổi trạng thái, hủy đăng ký và eviction khi mất heartbeat.
 * Mỗi test dùng tên app riêng vì các test dùng chung một registry.
 */
class RegistryLifecycleIT extends EurekaServerIntegrationTestSupport {

    @Test
    void registeredInstanceIsDiscoverableWithItsMetadata() {
        HttpResponse<String> registration = register("IT-METADATA-SERVICE", "it-metadata:1", 30,
                "{\"version\":\"2.4.0\",\"zone\":\"zone-a\"}");
        assertThat(registration.statusCode()).isEqualTo(204);

        HttpResponse<String> app = getAuthenticated("/eureka/apps/IT-METADATA-SERVICE");
        assertThat(app.statusCode()).isEqualTo(200);
        String body = app.body();
        assertThat(JsonPath.<String>read(body, "$.application.name")).isEqualTo("IT-METADATA-SERVICE");
        assertThat(JsonPath.<List<String>>read(body, "$.application.instance[*].instanceId"))
                .containsExactly("it-metadata:1");
        assertThat(JsonPath.<String>read(body, "$.application.instance[0].status")).isEqualTo("UP");
        assertThat(JsonPath.<String>read(body, "$.application.instance[0].hostName")).isEqualTo("localhost");
        assertThat(JsonPath.<Integer>read(body, "$.application.instance[0].port.$")).isEqualTo(18080);
        assertThat(JsonPath.<Map<String, String>>read(body, "$.application.instance[0].metadata"))
                .containsEntry("version", "2.4.0")
                .containsEntry("zone", "zone-a");

        // Tra cứu theo VIP address, cách DiscoveryClient tìm service theo tên logic.
        HttpResponse<String> vip = getAuthenticated("/eureka/vips/it-metadata-service");
        assertThat(vip.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<List<String>>read(vip.body(), "$.applications.application[*].instance[*].instanceId"))
                .contains("it-metadata:1");

        // Danh sách toàn bộ registry chứa app vừa đăng ký.
        assertThat(JsonPath.<List<String>>read(getAuthenticated("/eureka/apps").body(),
                "$.applications.application[*].name")).contains("IT-METADATA-SERVICE");
    }

    @Test
    void heartbeatsKeepInstanceRegisteredBeyondItsLeaseAndMissingHeartbeatsEvictIt() throws InterruptedException {
        String app = "IT-HEARTBEAT-SERVICE";
        String instance = "it-heartbeat:1";
        assertThat(register(app, instance, 2, "{}").statusCode()).isEqualTo(204);

        // Gửi heartbeat 12 lần trong ~6 giây, gấp 3 lần lease 2 giây: instance phải còn trong registry.
        for (int i = 0; i < 12; i++) {
            assertThat(heartbeat(app, instance).statusCode()).isEqualTo(200);
            Thread.sleep(500);
            assertThat(getInstance(app, instance).statusCode()).isEqualTo(200);
        }
        long lastRenewal = JsonPath.<Long>read(getInstance(app, instance).body(),
                "$.instance.leaseInfo.lastRenewalTimestamp");
        long registered = JsonPath.<Long>read(getInstance(app, instance).body(),
                "$.instance.leaseInfo.registrationTimestamp");
        assertThat(lastRenewal).isGreaterThan(registered);

        // Ngừng heartbeat: eviction task xóa hẳn instance (app biến mất khỏi registry).
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(250))
                .until(() -> getAuthenticated("/eureka/apps/" + app).statusCode() == 404);
    }

    @Test
    void instanceThatNeverSendsHeartbeatIsHiddenWhenExpiredThenEvictedAndMustRegisterAgain() {
        String app = "IT-SILENT-SERVICE";
        String instance = "it-silent:1";
        assertThat(register(app, instance, 1, "{}").statusCode()).isEqualTo(204);
        assertThat(getInstance(app, instance).statusCode()).isEqualTo(200);

        // 1) Lease hết hạn: tra cứu instance không còn trả về nó (client không bị điều hướng tới instance chết).
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(100))
                .until(() -> getInstance(app, instance).statusCode() == 404);

        // 2) Eviction task xóa lease khỏi registry: app biến mất khỏi danh sách.
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(250))
                .until(() -> getAuthenticated("/eureka/apps/" + app).statusCode() == 404);

        // 3) Heartbeat sau khi bị evict trả 404, tín hiệu để Eureka client tự đăng ký lại.
        assertThat(heartbeat(app, instance).statusCode()).isEqualTo(404);
    }

    @Test
    void cancelledInstanceIsRemovedImmediately() {
        String app = "IT-CANCEL-SERVICE";
        assertThat(register(app, "it-cancel:1", 30, "{}").statusCode()).isEqualTo(204);
        assertThat(register(app, "it-cancel:2", 30, "{}").statusCode()).isEqualTo(204);

        HttpResponse<String> cancel = send("DELETE", "/eureka/apps/" + app + "/it-cancel:1",
                basic(USERNAME, PASSWORD), null);

        assertThat(cancel.statusCode()).isEqualTo(200);
        assertThat(getInstance(app, "it-cancel:1").statusCode()).isEqualTo(404);
        assertThat(JsonPath.<List<String>>read(getAuthenticated("/eureka/apps/" + app).body(),
                "$.application.instance[*].instanceId")).containsExactly("it-cancel:2");
    }

    @Test
    void outOfServiceOverrideMarksInstanceUnavailableUntilRemoved() {
        String app = "IT-STATUS-SERVICE";
        String instance = "it-status:1";
        String path = "/eureka/apps/" + app + "/" + instance + "/status";
        assertThat(register(app, instance, 30, "{}").statusCode()).isEqualTo(204);

        assertThat(send("PUT", path + "?value=OUT_OF_SERVICE", basic(USERNAME, PASSWORD), null).statusCode())
                .isEqualTo(200);
        assertThat(JsonPath.<String>read(getInstance(app, instance).body(), "$.instance.status"))
                .isEqualTo("OUT_OF_SERVICE");

        assertThat(send("DELETE", path + "?value=UP", basic(USERNAME, PASSWORD), null).statusCode())
                .isEqualTo(200);
        assertThat(JsonPath.<String>read(getInstance(app, instance).body(), "$.instance.status"))
                .isEqualTo("UP");
    }

    @Test
    void heartbeatForUnknownInstanceIsRejected() {
        assertThat(heartbeat("IT-UNKNOWN-SERVICE", "it-unknown:1").statusCode()).isEqualTo(404);
    }
}
