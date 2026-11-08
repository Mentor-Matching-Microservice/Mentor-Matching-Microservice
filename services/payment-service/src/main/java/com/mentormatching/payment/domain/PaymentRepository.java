package com.mentormatching.payment.domain;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PaymentRepository extends JpaRepository<Payment,UUID> {
    Optional<Payment> findByPayerIdAndIdempotencyKey(UUID payerId,String key);
    Optional<Payment> findByBookingId(UUID bookingId);
    Optional<Payment> findByProviderReferenceId(String providerReferenceId);
    Optional<Payment> findByProviderTransactionId(String providerTransactionId);
    List<Payment> findTop100ByStatusAndExpiresAtBefore(PaymentStatus status,Instant before);
    org.springframework.data.domain.Page<Payment> findByPayerIdOrderByCreatedAtDesc(UUID payerId,org.springframework.data.domain.Pageable pageable);
}
