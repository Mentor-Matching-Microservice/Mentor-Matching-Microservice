package com.mentormatching.user.repository;

import com.mentormatching.user.entity.InboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboxEventRepository extends JpaRepository<InboxEvent, String> {
}
