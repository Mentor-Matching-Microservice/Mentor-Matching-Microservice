package com.mentormatching.payment.domain;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PaymentRepository extends JpaRepository<Payment,UUID> {
    Optional<Payment> findByPayerIdAndIdempotencyKey(UUID payerId,String key);
    Optional<Payment> findByBookingId(UUID bookingId);
    Optional<Payment> findByProviderReferenceId(String providerReferenceId);
    org.springframework.data.domain.Page<Payment> findByPayerIdOrderByCreatedAtDesc(UUID payerId,org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Payment> findByMentorIdOrderByCreatedAtDesc(UUID mentorId,org.springframework.data.domain.Pageable pageable);
}
