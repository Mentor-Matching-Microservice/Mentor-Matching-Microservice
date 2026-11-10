package com.mentormatching.payment.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {
    java.util.Optional<PaymentAttempt> findByProviderReferenceId(String providerReferenceId);
}
