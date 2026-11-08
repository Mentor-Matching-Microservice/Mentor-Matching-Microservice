package com.mentormatching.payment.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingEventReceiptRepository extends JpaRepository<BookingEventReceipt, String> {
}
