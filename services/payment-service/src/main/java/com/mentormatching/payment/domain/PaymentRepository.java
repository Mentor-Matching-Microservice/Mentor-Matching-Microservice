package com.mentormatching.payment.domain;
import java.time.Instant;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface PaymentRepository extends JpaRepository<Payment,UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") UUID id);
    Optional<Payment> findByPayerIdAndIdempotencyKey(UUID payerId,String key);
    Optional<Payment> findByBookingId(UUID bookingId);
    Optional<Payment> findByProviderReferenceId(String providerReferenceId);
    Optional<Payment> findByProviderTransactionId(String providerTransactionId);
    List<Payment> findTop100ByStatusAndExpiresAtBefore(PaymentStatus status,Instant before);
    org.springframework.data.domain.Page<Payment> findByPayerIdOrderByCreatedAtDesc(UUID payerId,org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Payment> findByMentorIdOrderByCreatedAtDesc(UUID mentorId,org.springframework.data.domain.Pageable pageable);
}
