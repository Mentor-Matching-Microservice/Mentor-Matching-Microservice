package com.mentormatching.stats.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import com.mentormatching.stats.config.StatsProperties;
import com.mentormatching.stats.domain.DailyStats;
import com.mentormatching.stats.domain.Metric;
import com.mentormatching.stats.domain.MetricSource;
import com.mentormatching.stats.domain.ProcessedEvent;
import com.mentormatching.stats.event.DomainEvent;
import com.mongodb.MongoException;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Cộng một sự kiện vào bộ đếm của ngày xảy ra ({@code occurredAt} theo {@code stats.zone}).
 * Bản ghi {@code processed_events} và {@code $inc} nằm trong cùng một transaction MongoDB, nên nhận lại
 * cùng sự kiện (redeliver, hai consumer đồng thời) không bị cộng hai lần.
 *
 * <p>Hoàn tiền: {@code refundedAmount} trong {@code PaymentRefunded} là TỔNG đã hoàn của khoản thanh toán,
 * nên chỉ cộng phần chênh lệch so với lần trước (collection {@code payment_refund_totals}).
 */
@Service
@EnableConfigurationProperties(StatsProperties.class)
public class StatsIngestionService {

    public enum Outcome { COUNTED, DUPLICATE, IGNORED }

    static final int MAX_TRANSACTION_ATTEMPTS = 5;
    static final String REFUND_TOTALS = "payment_refund_totals";

    private final MongoTemplate mongo;
    private final TransactionTemplate transaction;
    private final StatsProperties properties;
    private final Clock clock;

    public StatsIngestionService(MongoTemplate mongo, PlatformTransactionManager transactionManager,
                                 StatsProperties properties, Clock clock) {
        this.mongo = mongo;
        this.transaction = new TransactionTemplate(transactionManager);
        this.properties = properties;
        this.clock = clock;
    }

    public Outcome ingest(DomainEvent event) {
        Metric metric = Metric.forEventType(event.eventType());
        if (metric == null) {
            return Outcome.IGNORED;
        }
        Instant now = Instant.now(clock);
        LocalDate day = (event.occurredAt() != null ? event.occurredAt() : now).atZone(properties.zone()).toLocalDate();
        for (int attempt = 1; ; attempt++) {
            try {
                return transaction.execute(status -> apply(event, metric, day, now));
            } catch (DuplicateKeyException ex) {
                return Outcome.DUPLICATE; // một lần giao khác đã commit; transaction này rollback toàn bộ
            } catch (RuntimeException ex) {
                if (attempt >= MAX_TRANSACTION_ATTEMPTS || !isTransientTransactionError(ex)) {
                    throw ex;
                }
            }
        }
    }

    private Outcome apply(DomainEvent event, Metric metric, LocalDate day, Instant now) {
        if (mongo.exists(Query.query(Criteria.where("_id").is(event.eventId())), ProcessedEvent.class)) {
            return Outcome.DUPLICATE;
        }
        Update update = new Update().inc(metric.field(), 1);
        if (metric == Metric.PAYMENTS_SUCCEEDED && inReportingCurrency(event)) {
            BigDecimal amount = event.optionalDecimal("amount");
            if (amount != null && amount.signum() > 0) {
                update.inc("revenue", new Decimal128(amount));
            }
        }
        if (metric == Metric.PAYMENTS_REFUNDED && inReportingCurrency(event)) {
            BigDecimal delta = refundDelta(event);
            if (delta.signum() > 0) {
                update.inc("refunded", new Decimal128(delta));
            }
        }
        mongo.upsert(Query.query(Criteria.where("_id").is(day.toString())), update, DailyStats.class);
        mongo.insert(new ProcessedEvent(event.eventId(), event.eventType(), now));
        mongo.upsert(Query.query(Criteria.where("_id").is(event.eventType())),
                new Update().setOnInsert("firstSeenAt", now), MetricSource.class);
        return Outcome.COUNTED;
    }

    /** Phần hoàn tiền mới so với tổng đã ghi nhận trước đó của cùng khoản thanh toán. */
    private BigDecimal refundDelta(DomainEvent event) {
        BigDecimal total = event.optionalDecimal("refundedAmount");
        if (total == null || total.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        String paymentId = event.optionalText("paymentId");
        if (paymentId == null) {
            return total;
        }
        Query byPayment = Query.query(Criteria.where("_id").is(paymentId));
        Document previous = mongo.findOne(byPayment, Document.class, REFUND_TOTALS);
        BigDecimal before = previous != null && previous.get("refunded") instanceof Decimal128 d
                ? d.bigDecimalValue() : BigDecimal.ZERO;
        if (total.compareTo(before) <= 0) {
            return BigDecimal.ZERO; // sự kiện cũ tới muộn: không làm giảm số đã cộng
        }
        mongo.upsert(byPayment, new Update().set("refunded", new Decimal128(total)), REFUND_TOTALS);
        return total.subtract(before);
    }

    private boolean inReportingCurrency(DomainEvent event) {
        String currency = event.optionalText("currency");
        return currency == null || properties.currency().equalsIgnoreCase(currency);
    }

    static boolean isTransientTransactionError(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof MongoException mongo
                    && mongo.hasErrorLabel(MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL)) {
                return true;
            }
        }
        return false;
    }
}
