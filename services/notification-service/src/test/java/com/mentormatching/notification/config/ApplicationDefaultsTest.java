package com.mentormatching.notification.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

/**
 * Giá trị mặc định trong application.yml: đúng cổng/tên service theo kế hoạch, không có secret,
 * Eureka tắt cho tới khi được bật qua biến môi trường, actuator chỉ mở health/info.
 */
class ApplicationDefaultsTest {

    private final PropertySource<?> yaml = load();

    @Test
    void identityMatchesThePlan() {
        assertThat(value("server.port")).isEqualTo("${SERVER_PORT:8089}");
        assertThat(value("spring.application.name")).isEqualTo("notification-service");
        assertThat(value("spring.mongodb.uri")).contains("/mm_notification").contains("replicaSet=");
    }

    @Test
    void secretsAndEnvironmentSpecificValuesComeFromEnvironment() {
        assertThat(value("notification.jwt.audience")).isEqualTo("${AUTH_JWT_AUDIENCE:}");
        assertThat(value("notification.jwt.public-key-location")).isEqualTo("${AUTH_JWT_PUBLIC_KEY_LOCATION:}");
        assertThat(value("spring.mongodb.uri")).startsWith("${MONGODB_URI:").doesNotContain("@");
    }

    @Test
    void discoveryIsOptInAndDoesNotLogRegistryCredentials() {
        assertThat(value("eureka.client.enabled")).isEqualTo("${EUREKA_ENABLED:false}");
        assertThat(value("logging.level.com.netflix.discovery.shared.transport.decorator.RedirectingEurekaHttpClient"))
                .isEqualTo("WARN");
    }

    @Test
    void managementSurfaceIsMinimal() {
        assertThat(value("management.endpoints.web.exposure.include")).isEqualTo("health,info");
        assertThat(value("management.endpoint.health.show-details")).isEqualTo("never");
        assertThat(value("springdoc.api-docs.enabled")).isEqualTo("${API_DOCS_ENABLED:false}");
    }

    private String value(String key) {
        Object value = yaml.getProperty(key);
        return value == null ? null : value.toString();
    }

    private static PropertySource<?> load() {
        try {
            List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                    .load("application", new ClassPathResource("application.yml"));
            return sources.getFirst();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
