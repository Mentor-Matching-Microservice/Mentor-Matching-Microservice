package com.mentormatching.auth.repository;

import com.mentormatching.auth.entity.InboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InboxEventRepository extends JpaRepository<InboxEvent, String> {
}
