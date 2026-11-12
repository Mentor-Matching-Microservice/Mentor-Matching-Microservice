package com.mentormatching.booking.repository;

import com.mentormatching.booking.entity.InboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InboxEventRepository extends JpaRepository<InboxEvent, String> {
}
