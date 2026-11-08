package com.mentormatching.payment.domain;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RefundTransactionRepository extends JpaRepository<RefundTransaction,UUID>{ Optional<RefundTransaction> findByIdempotencyKey(String key); }
