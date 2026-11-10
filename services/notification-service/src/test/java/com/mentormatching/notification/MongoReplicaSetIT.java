package com.mentormatching.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import com.mentormatching.notification.support.NotificationIntegrationTestSupport;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * MongoDB: đúng database {@code mm_notification}, health có kiểm tra Mongo và transaction
 * hoạt động trên replica set (nền cho inbox dedup + lưu thông báo nguyên tử ở #73/#81).
 */
class MongoReplicaSetIT extends NotificationIntegrationTestSupport {

    private static final String COLLECTION = "it_transaction_probe";

    @Autowired
    private MongoTemplate mongo;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private HealthEndpoint healthEndpoint;

    @BeforeEach
    void cleanCollection() {
        if (!mongo.collectionExists(COLLECTION)) {
            mongo.createCollection(COLLECTION); // transaction không tự tạo collection trên mọi phiên bản
        }
        mongo.remove(new Query(), COLLECTION);
    }

    @Test
    void connectsToTheNotificationDatabase() {
        assertThat(mongo.getDb().getName()).isEqualTo(DATABASE);
    }

    @Test
    void mongoHealthContributorIsUp() {
        var mongoHealth = healthEndpoint.healthForPath("mongo");
        assertThat(mongoHealth).isNotNull();
        assertThat(mongoHealth.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void usesMongoTransactionManager() {
        assertThat(transactionManager).isInstanceOf(MongoTransactionManager.class);
    }

    @Test
    void committedTransactionPersistsAllWrites() {
        String key = UUID.randomUUID().toString();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            mongo.insert(new Document("key", key).append("part", "inbox"), COLLECTION);
            mongo.insert(new Document("key", key).append("part", "notification"), COLLECTION);
        });
        assertThat(count(key)).isEqualTo(2);
    }

    @Test
    void failedTransactionRollsBackEveryWrite() {
        String key = UUID.randomUUID().toString();
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            mongo.insert(new Document("key", key).append("part", "inbox"), COLLECTION);
            throw new IllegalStateException("simulated failure after first write");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(count(key)).isZero();
    }

    private long count(String key) {
        return mongo.count(new Query(Criteria.where("key").is(key)), COLLECTION);
    }
}
