package com.mentormatching.notification.repository;

import com.mentormatching.notification.domain.InboxEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InboxEventRepository extends MongoRepository<InboxEvent, String> {
}
