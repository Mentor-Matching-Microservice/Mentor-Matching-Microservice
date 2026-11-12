package com.mentormatching.notification.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.mentormatching.notification.domain.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

class DomainEventAndMappersTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void parsesFlatEnvelopeUsedByOtherServices() {
        DomainEvent event = parse("""
                {"eventId":" e-1 ","eventType":"AccountRegistered","occurredAt":"2026-11-10T10:00:00Z","accountId":"%s"}
                """.formatted(UUID.randomUUID()));
        assertThat(event.eventId()).isEqualTo("e-1");
        assertThat(event.eventType()).isEqualTo("AccountRegistered");
        assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-11-10T10:00:00Z"));
    }

    @Test
    void occurredAtIsOptionalAndToleratesBadFormat() {
        assertThat(parse("{\"eventId\":\"e\",\"eventType\":\"T\"}").occurredAt()).isNull();
        assertThat(parse("{\"eventId\":\"e\",\"eventType\":\"T\",\"occurredAt\":\"yesterday\"}").occurredAt()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{not json", "[]", "\"text\"", "{}", "{\"eventType\":\"T\"}", "{\"eventId\":\"e\"}",
            "{\"eventId\":\" \",\"eventType\":\"T\"}", "{\"eventId\":1,\"eventType\":\"T\"}"})
    void rejectsMalformedEnvelopes(String body) {
        assertThatThrownBy(() -> parse(body)).isInstanceOf(InvalidEventException.class);
    }

    @Test
    void rejectsOverlongIdentifiers() {
        String longId = "x".repeat(DomainEvent.MAX_ID_LENGTH + 1);
        assertThatThrownBy(() -> parse("{\"eventId\":\"" + longId + "\",\"eventType\":\"T\"}"))
                .isInstanceOf(InvalidEventException.class);
    }

    @Test
    void errorMessagesNameFieldsButNeverEchoValues() {
        String secretLike = "not-a-uuid-but-sensitive-value";
        DomainEvent event = parse("{\"eventId\":\"e\",\"eventType\":\"T\",\"accountId\":\"" + secretLike + "\"}");
        assertThatThrownBy(() -> event.requiredUuid("accountId"))
                .isInstanceOf(InvalidEventException.class)
                .hasMessage("accountId must be a UUID")
                .hasMessageNotContaining(secretLike);
    }

    @Test
    void requiredUuidAcceptsAliasesInOrder() {
        UUID id = UUID.randomUUID();
        assertThat(parse("{\"eventId\":\"e\",\"eventType\":\"T\",\"userId\":\"" + id + "\"}")
                .requiredUuid("accountId", "userId")).isEqualTo(id);
        assertThatThrownBy(() -> parse("{\"eventId\":\"e\",\"eventType\":\"T\"}").requiredUuid("accountId", "userId"))
                .hasMessage("Missing required field accountId|userId");
    }

    @Test
    void accountRegisteredWelcomesTheNewAccount() {
        UUID account = UUID.randomUUID();
        List<NotificationDraft> drafts = new AccountEventMapper().map(
                parse("{\"eventId\":\"e\",\"eventType\":\"AccountRegistered\",\"accountId\":\"" + account + "\"}"));

        assertThat(drafts).singleElement().satisfies(draft -> {
            assertThat(draft.recipientUserId()).isEqualTo(account);
            assertThat(draft.type()).isEqualTo(NotificationType.ACCOUNT_REGISTERED);
            assertThat(draft.title()).isNotBlank();
        });
    }

    @Test
    void mentorDecisionsNotifyTheApplicant() {
        UUID user = UUID.randomUUID();
        MentorVerificationEventMapper mentor = new MentorVerificationEventMapper();

        NotificationDraft approved = mentor.map(parse("{\"eventId\":\"e\",\"eventType\":\"MentorApproved\","
                + "\"mentorUserId\":\"" + user + "\",\"mentorId\":9}")).getFirst();
        assertThat(approved.type()).isEqualTo(NotificationType.MENTOR_APPROVED);
        assertThat(approved.recipientUserId()).isEqualTo(user);
        assertThat(approved.referenceId()).isEqualTo("9");

        NotificationDraft rejected = mentor.map(parse("{\"eventId\":\"e\",\"eventType\":\"MentorRejected\","
                + "\"userId\":\"" + user + "\",\"reason\":\"Thiếu chứng chỉ\"}")).getFirst();
        assertThat(rejected.type()).isEqualTo(NotificationType.MENTOR_REJECTED);
        assertThat(rejected.message()).contains("Lý do: Thiếu chứng chỉ");
        assertThat(rejected.referenceId()).as("falls back to the user id").isEqualTo(user.toString());

        NotificationDraft noReason = mentor.map(parse("{\"eventId\":\"e\",\"eventType\":\"MentorRejected\","
                + "\"mentorUserId\":\"" + user + "\"}")).getFirst();
        assertThat(noReason.message()).doesNotContain("Lý do");
    }

    @Test
    void longRejectionReasonsAreTruncated() {
        String reason = "a".repeat(2000);
        NotificationDraft draft = new MentorVerificationEventMapper().map(parse("{\"eventId\":\"e\",\"eventType\":"
                + "\"MentorRejected\",\"mentorUserId\":\"" + UUID.randomUUID() + "\",\"reason\":\"" + reason + "\"}"))
                .getFirst();
        assertThat(draft.message().length()).isLessThan(700);
    }

    private DomainEvent parse(String json) {
        return DomainEvent.parse(json.strip().getBytes(StandardCharsets.UTF_8), mapper);
    }
}
