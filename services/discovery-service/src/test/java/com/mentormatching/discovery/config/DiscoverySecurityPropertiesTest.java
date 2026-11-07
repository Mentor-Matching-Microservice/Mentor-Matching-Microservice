package com.mentormatching.discovery.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DiscoverySecurityPropertiesTest {

    private static final String VALID_PASSWORD = "unit-test-password-0001";

    @Test
    void acceptsValidCredentials() {
        var properties = new DiscoverySecurityProperties("discovery-client", VALID_PASSWORD);

        assertThat(properties.username()).isEqualTo("discovery-client");
        assertThat(properties.password()).isEqualTo(VALID_PASSWORD);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsMissingUsernameAndNamesTheEnvironmentVariable(String username) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DiscoverySecurityProperties(username, VALID_PASSWORD))
                .withMessageContaining("DISCOVERY_USERNAME");
    }

    @ParameterizedTest
    @ValueSource(strings = {"user:name", "user name", "user/name", "người-dùng"})
    void rejectsUsernameThatCannotBeUsedSafelyInHttpBasicOrServiceUrl(String username) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DiscoverySecurityProperties(username, VALID_PASSWORD))
                .withMessageContaining("discovery.security.username");
    }

    @Test
    void rejectsOverlongUsername() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DiscoverySecurityProperties("u".repeat(65), VALID_PASSWORD));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"    "})
    void rejectsMissingPasswordAndNamesTheEnvironmentVariable(String password) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DiscoverySecurityProperties("discovery", password))
                .withMessageContaining("DISCOVERY_PASSWORD");
    }

    @Test
    void rejectsShortPasswordWithoutEchoingIt() {
        String shortPassword = "Short-Secret-1";

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DiscoverySecurityProperties("discovery", shortPassword))
                .withMessageContaining("at least 16 characters")
                .withMessageNotContaining(shortPassword);
    }

    @Test
    void rejectsPasswordLongerThanBcryptLimitInBytes() {
        // 30 ký tự nhưng 90 byte UTF-8: vượt giới hạn 72 byte của BCrypt.
        String multiBytePassword = "ệ".repeat(30);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DiscoverySecurityProperties("discovery", multiBytePassword))
                .withMessageContaining("72 bytes");
    }

    @Test
    void rejectsEnvExamplePlaceholderPassword() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DiscoverySecurityProperties("discovery", "replace-with-a-strong-random-secret"))
                .withMessageContaining("placeholder");
    }

    @Test
    void toStringNeverContainsPassword() {
        var properties = new DiscoverySecurityProperties("discovery", VALID_PASSWORD);

        assertThat(properties.toString())
                .contains("discovery")
                .doesNotContain(VALID_PASSWORD);
    }
}
