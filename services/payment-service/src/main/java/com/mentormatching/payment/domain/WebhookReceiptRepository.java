package com.mentormatching.payment.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookReceiptRepository extends JpaRepository<WebhookReceipt, String> {
}
