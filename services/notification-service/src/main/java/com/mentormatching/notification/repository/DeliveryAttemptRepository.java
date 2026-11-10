package com.mentormatching.notification.repository;

import java.util.List;

import com.mentormatching.notification.domain.DeliveryAttempt;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DeliveryAttemptRepository extends MongoRepository<DeliveryAttempt, String> {

    List<DeliveryAttempt> findByNotificationIdOrderByAttemptAsc(String notificationId);
}
