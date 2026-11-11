package com.mentormatching.discovery.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Kiểm tra binding cấu hình bắt buộc và cách lưu credential, không cần web server.
 */
class DiscoveryCredentialsConfigurationTest {

    private static final String PASSWORD = "context-test-password-01";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(DiscoveryCredentialsConfiguration.class);

    @Test
    void createsSingleRegistryAccountWithBcryptHashedPassword() {
        runner.withPropertyValues("discovery.security.username=registry-client",
                        "discovery.security.password=" + PASSWORD)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var user = context.getBean(UserDetailsService.class).loadUserByUsername("registry-client");
                    var encoder = context.getBean(PasswordEncoder.class);

                    assertThat(user.getPassword()).startsWith("{bcrypt}").doesNotContain(PASSWORD);
                    assertThat(encoder.matches(PASSWORD, user.getPassword())).isTrue();
                    assertThat(encoder.matches("wrong-password-000000", user.getPassword())).isFalse();
                    assertThat(user.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                            .containsExactly("ROLE_" + DiscoveryCredentialsConfiguration.REGISTRY_ROLE);
                    assertThat(user.isEnabled()).isTrue();
                });
    }

    @Test
    void failsToStartWhenCredentialsAreMissing() {
        runner.run(context -> assertThat(context).hasFailed()
                .getFailure().rootCause().hasMessageContaining("DISCOVERY_USERNAME"));
    }

    @Test
    void failsToStartWhenOnlyPasswordIsMissing() {
        runner.withPropertyValues("discovery.security.username=registry-client")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().rootCause().hasMessageContaining("DISCOVERY_PASSWORD"));
    }

    @Test
    void failureForWeakPasswordDoesNotExposeTheValue() {
        String weakPassword = "weakValue-123";

        runner.withPropertyValues("discovery.security.username=registry-client",
                        "discovery.security.password=" + weakPassword)
                .run(context -> {
                    assertThat(context).hasFailed();
                    Throwable failure = context.getStartupFailure();
                    for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                        assertThat(String.valueOf(cause.getMessage())).doesNotContain(weakPassword);
                    }
                });
    }
}
