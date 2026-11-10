package com.mentormatching.stats.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Bộ đếm của một ngày (theo múi giờ {@code stats.zone}), {@code _id = "yyyy-MM-dd"}.
 * Chỉ được cập nhật bằng {@code $inc} nguyên tử khi nhận sự kiện; tiền lưu Decimal128.
 */
@Document("daily_stats")
public class DailyStats {

    @Id
    private String date;
    private long newUsers;
    private long newMentors;
    private long bookingsCreated;
    private long bookingsConfirmed;
    private long bookingsCancelled;
    private long bookingsRejected;
    private long bookingsConfirmationFailed;
    private long bookingsCompleted;
    private long paymentsSucceeded;
    private long paymentsFailed;
    private long paymentsRefunded;
    private BigDecimal revenue;
    private BigDecimal refunded;

    protected DailyStats() {
        // dành cho Spring Data
    }

    public LocalDate getDate() { return LocalDate.parse(date); }
    public long getNewUsers() { return newUsers; }
    public long getNewMentors() { return newMentors; }
    public long getBookingsCreated() { return bookingsCreated; }
    public long getBookingsConfirmed() { return bookingsConfirmed; }
    public long getBookingsCancelled() { return bookingsCancelled; }
    public long getBookingsRejected() { return bookingsRejected; }
    public long getBookingsConfirmationFailed() { return bookingsConfirmationFailed; }
    public long getBookingsCompleted() { return bookingsCompleted; }
    public long getPaymentsSucceeded() { return paymentsSucceeded; }
    public long getPaymentsFailed() { return paymentsFailed; }
    public long getPaymentsRefunded() { return paymentsRefunded; }
    public BigDecimal getRevenue() { return revenue == null ? BigDecimal.ZERO : revenue; }
    public BigDecimal getRefunded() { return refunded == null ? BigDecimal.ZERO : refunded; }
}
