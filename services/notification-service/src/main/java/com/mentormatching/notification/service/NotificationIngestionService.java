package com.mentormatching.notification.service;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mentormatching.notification.domain.InboxEvent;
import com.mentormatching.notification.domain.Notification;
import com.mentormatching.notification.dto.NotificationResponse;
import com.mentormatching.notification.event.DomainEvent;
import com.mentormatching.notification.event.EventNotificationMapper;
import com.mentormatching.notification.event.NotificationDraft;
import com.mentormatching.notification.repository.InboxEventRepository;
import com.mentormatching.notification.repository.NotificationRepository;
import com.mongodb.MongoException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Biến sự kiện thành thông báo, chống trùng theo {@code eventId} (inbox) và lưu nguyên tử:
 * bản ghi inbox + toàn bộ thông báo của sự kiện nằm trong cùng một transaction MongoDB.
 * Nhận lại cùng một sự kiện (publisher gửi lại, broker redeliver, hai consumer cùng lúc) không tạo thêm thông báo.
 */
@Service
public class NotificationIngestionService {

    public enum Outcome { CREATED, DUPLICATE, IGNORED }

    static final int MAX_TRANSACTION_ATTEMPTS = 5;

    private final Map<String, EventNotificationMapper> mappersByType;
    private final NotificationRepository notifications;
    private final InboxEventRepository inbox;
    private final RealtimeNotificationPublisher realtime;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public NotificationIngestionService(List<EventNotificationMapper> mappers, NotificationRepository notifications,
                                        InboxEventRepository inbox, RealtimeNotificationPublisher realtime,
                                        PlatformTransactionManager transactionManager, Clock clock) {
        this.mappersByType = index(mappers);
        this.notifications = notifications;
        this.inbox = inbox;
        this.realtime = realtime;
        this.transaction = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    public boolean supports(String eventType) {
        return mappersByType.containsKey(eventType);
    }

    /**
     * @return {@code IGNORED} nếu không có mapper cho loại sự kiện, {@code DUPLICATE} nếu đã xử lý trước đó
     * @throws com.mentormatching.notification.event.InvalidEventException nếu payload thiếu trường bắt buộc
     */
    public Outcome ingest(DomainEvent event) {
        EventNotificationMapper mapper = mappersByType.get(event.eventType());
        if (mapper == null) {
            return Outcome.IGNORED;
        }
        List<NotificationDraft> drafts = mapper.map(event); // kiểm tra payload trước khi mở transaction
        for (int attempt = 1; ; attempt++) {
            try {
                return persistOnce(event, drafts);
            } catch (DuplicateKeyException ex) {
                // Một lần giao khác của cùng sự kiện đã commit trước; transaction này đã rollback toàn bộ.
                return Outcome.DUPLICATE;
            } catch (RuntimeException ex) {
                // Hai transaction ghi cùng eventId cùng lúc: MongoDB huỷ một bên với nhãn TransientTransactionError.
                // Thử lại theo khuyến nghị của MongoDB; lần sau sẽ thấy bản ghi inbox và trả về DUPLICATE.
                if (attempt >= MAX_TRANSACTION_ATTEMPTS || !isTransientTransactionError(ex)) {
                    throw ex;
                }
            }
        }
    }

    private Outcome persistOnce(DomainEvent event, List<NotificationDraft> drafts) {
        return transaction.execute(status -> {
            if (inbox.existsById(event.eventId())) {
                return Outcome.DUPLICATE;
            }
            Instant now = Notification.toStoredPrecision(Instant.now(clock));
            List<Notification> created = drafts.stream()
                    .map(draft -> Notification.create(draft.recipientUserId(), draft.type(), draft.title(),
                            draft.message(), draft.referenceType(), draft.referenceId(), event.eventId(), now))
                    .toList();
            notifications.insert(created);
            inbox.insert(new InboxEvent(event.eventId(), event.eventType(), created.size(), now));
            realtime.publishAfterCommit(created.stream().map(NotificationResponse::from).toList());
            return Outcome.CREATED;
        });
    }

    static boolean isTransientTransactionError(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof MongoException mongo && mongo.hasErrorLabel(MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, EventNotificationMapper> index(List<EventNotificationMapper> mappers) {
        Map<String, EventNotificationMapper> byType = new HashMap<>();
        for (EventNotificationMapper mapper : mappers) {
            for (String type : mapper.eventTypes()) {
                EventNotificationMapper previous = byType.putIfAbsent(type, mapper);
                if (previous != null) {
                    throw new IllegalStateException("Event type " + type + " is mapped twice: "
                            + previous.getClass().getSimpleName() + " and " + mapper.getClass().getSimpleName());
                }
            }
        }
        return Map.copyOf(byType);
    }
}
