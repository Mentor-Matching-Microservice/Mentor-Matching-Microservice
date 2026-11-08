package com.mentormatching.payment.domain;
import org.springframework.data.jpa.repository.JpaRepository;
public interface StripeWebhookEventRepository extends JpaRepository<StripeWebhookEvent,String> {}
