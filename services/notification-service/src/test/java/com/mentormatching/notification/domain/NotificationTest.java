package com.mentormatching.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import com.mentormatching.notification.service.NotificationIngestionServiceTestAccess;
import com.mongodb.MongoException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class NotificationTest {

    private final UUID owner = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-11-10T10:00:00.123456789Z");

    @Test
    void newNotificationIsUnreadWithUuidIdAndMillisecondTimestamps() {
        Notification n = Notification.create(owner, NotificationType.SYSTEM, " Title ", " Body ", null, null, null, now);

        assertThat(UUID.fromString(n.getId())).isNotNull();
        assertThat(n.isRead()).isFalse();
        assertThat(n.getTitle()).isEqualTo("Title");
        assertThat(n.getCreatedAt()).isEqualTo(Instant.parse("2026-11-10T10:00:00.123Z"));
        assertThat(n.isOwnedBy(owner)).isTrue();
        assertThat(n.isOwnedBy(UUID.randomUUID())).isFalse();
        assertThat(n.isOwnedBy(null)).isFalse();
    }

    @Test
    void markReadKeepsTheFirstReadTime() {
        Notification n = Notification.create(owner, NotificationType.SYSTEM, "t", "m", null, null, null, now);
        n.markRead(now.plusSeconds(5));
        n.markRead(now.plusSeconds(60));
        assertThat(n.getReadAt()).isEqualTo(Notification.toStoredPrecision(now.plusSeconds(5)));
    }

    @Test
    void longTextIsTruncatedAndBlankTextRejected() {
        Notification n = Notification.create(owner, NotificationType.SYSTEM, "t".repeat(500), "m".repeat(5000),
                null, null, null, now);
        assertThat(n.getTitle()).hasSize(Notification.TITLE_MAX_LENGTH);
        assertThat(n.getMessage()).hasSize(Notification.MESSAGE_MAX_LENGTH);
        assertThatThrownBy(() -> Notification.create(owner, NotificationType.SYSTEM, " ", "m", null, null, null, now))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notification.create(null, NotificationType.SYSTEM, "t", "m", null, null, null, now))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void onlyTransientTransactionErrorsAreRetriedByIngestion() {
        MongoException conflict = new MongoException("write conflict");
        conflict.addLabel(MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL);
        assertThat(NotificationIngestionServiceTestAccess.isTransient(
                new DataIntegrityViolationException("wrapped", conflict))).isTrue();
        assertThat(NotificationIngestionServiceTestAccess.isTransient(new MongoException("other"))).isFalse();
        assertThat(NotificationIngestionServiceTestAccess.isTransient(new IllegalStateException())).isFalse();
    }
}
