package com.mentormatching.auth.repository;

import java.util.List;
import java.util.UUID;

import com.mentormatching.auth.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Lô sự kiện chưa publish, cũ nhất trước. {@code FOR UPDATE SKIP LOCKED}: khi chạy nhiều instance, mỗi sự kiện
     * chỉ được một instance lấy, các instance khác bỏ qua thay vì chờ.
     */
    @Query(value = "SELECT * FROM outbox_events WHERE published_at IS NULL ORDER BY created_at LIMIT 100 "
            + "FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxEvent> lockNextBatch();

    List<OutboxEvent> findByAggregateIdOrderByCreatedAtAsc(UUID aggregateId);

    long countByPublishedAtIsNull();
}
