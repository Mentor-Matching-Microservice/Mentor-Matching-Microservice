package com.mentormatching.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import com.mentormatching.auth.mail.AccountMailProperties;
import com.mentormatching.auth.service.AccountTokenProperties;
import com.mentormatching.auth.service.OneTimeTokens;
import org.junit.jupiter.api.Test;

class AccountTokenAndMailConfigTest {

    @Test
    void oneTimeTokensAreRandomWellFormedAndOnlyTheHashIsStored() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            String raw = OneTimeTokens.generate();
            assertThat(OneTimeTokens.wellFormed(raw)).isTrue();
            assertThat(seen.add(raw)).isTrue();
            assertThat(OneTimeTokens.hash(raw)).hasSize(64).doesNotContain(raw);
        }
        assertThat(OneTimeTokens.wellFormed("short")).isFalse();
        assertThat(OneTimeTokens.wellFormed(null)).isFalse();
        assertThat(OneTimeTokens.wellFormed("x".repeat(42) + "=")).isFalse();
    }

    @Test
    void tokenLifetimesHaveSafeDefaultsAndBounds() {
        AccountTokenProperties defaults = new AccountTokenProperties(null, null, null);
        assertThat(defaults.emailVerificationTtl()).isEqualTo(Duration.ofHours(24));
        assertThat(defaults.passwordResetTtl()).isEqualTo(Duration.ofMinutes(30));
        assertThat(defaults.resendCooldown()).isEqualTo(Duration.ofSeconds(60));
        assertThatThrownBy(() -> new AccountTokenProperties(Duration.ofDays(30), null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AccountTokenProperties(null, Duration.ofDays(2), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AccountTokenProperties(null, Duration.ZERO, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mailLinksCarryTheTokenAndDevLoggingCannotBeCombinedWithRealDelivery() {
        AccountMailProperties props = new AccountMailProperties(false, null,
                URI.create("https://app.example/verify-email"), URI.create("https://app.example/reset?lang=vi"), false);
        assertThat(props.verifyLink("abc")).isEqualTo("https://app.example/verify-email?token=abc");
        assertThat(props.resetLink("abc")).isEqualTo("https://app.example/reset?lang=vi&token=abc");
        assertThat(new AccountMailProperties(null, null, null, null, null).enabled()).isFalse();
        assertThatThrownBy(() -> new AccountMailProperties(true, null, null, null, true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
