package com.mentormatching.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.ZoneId;
import java.util.List;

import com.mentormatching.stats.config.StatsProperties;
import com.mentormatching.stats.domain.Metric;
import com.mentormatching.stats.messaging.MessagingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

class StatsConfigurationTest {

    @Test
    void metricsMapToTheEventTypesOtherServicesPublish() {
        assertThat(Metric.forEventType("PaymentSucceeded")).isEqualTo(Metric.PAYMENTS_SUCCEEDED);
        assertThat(Metric.forEventType("BookingConfirmed")).isEqualTo(Metric.BOOKINGS_CONFIRMED);
        assertThat(Metric.forEventType("MentorRatingUpdated")).isNull();
        // Chỉ các sự kiện đã được publish trên develop mới mặc định có dữ liệu.
        assertThat(Metric.PAYMENTS_SUCCEEDED.alwaysPublished()).isTrue();
        assertThat(Metric.BOOKINGS_CREATED.alwaysPublished()).isFalse();
        assertThat(Metric.NEW_USERS.alwaysPublished()).isFalse();
    }

    @Test
    void defaultsUseVietnamTimeVndAndAPrivateQueue() {
        StatsProperties stats = new StatsProperties(null, " vnd ");
        assertThat(stats.zone()).isEqualTo(ZoneId.of("Asia/Ho_Chi_Minh"));
        assertThat(stats.currency()).isEqualTo("VND");

        MessagingProperties messaging = new MessagingProperties(null, null, null, null, null);
        assertThat(messaging.exchange()).isEqualTo("mentor.events");
        assertThat(messaging.queue()).isEqualTo("stats.domain-events");
        assertThat(messaging.routingKeys()).containsExactly("booking.#", "payment.#", "auth.#", "mentor.#");
        assertThat(messaging.deadLetterQueue()).isEqualTo("stats.domain-events.dead");
        assertThatThrownBy(() -> new MessagingProperties(true, "x", "q", List.of("a"), 11))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void applicationDefaultsHaveNoSecretsAndAFreePort() throws Exception {
        PropertySource<?> yaml = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml")).getFirst();
        assertThat(yaml.getProperty("server.port")).hasToString("${SERVER_PORT:8090}");
        assertThat(yaml.getProperty("spring.application.name")).hasToString("stats-service");
        assertThat(yaml.getProperty("spring.mongodb.uri").toString()).contains("/mm_stats").doesNotContain("@");
        assertThat(yaml.getProperty("spring.data.mongodb.representation.big-decimal")).hasToString("decimal128");
        assertThat(yaml.getProperty("stats.jwt.audience")).hasToString("${AUTH_JWT_AUDIENCE:}");
        assertThat(yaml.getProperty("eureka.client.enabled")).hasToString("${EUREKA_ENABLED:false}");
        assertThat(yaml.getProperty("management.endpoints.web.exposure.include")).hasToString("health,info");
    }
}
