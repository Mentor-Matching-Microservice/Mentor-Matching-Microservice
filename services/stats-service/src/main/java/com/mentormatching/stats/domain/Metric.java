package com.mentormatching.stats.domain;

/**
 * Bộ đếm theo ngày và sự kiện nguồn của chúng. {@code alwaysPublished} = sự kiện nguồn đã được service
 * phụ trách publish trên {@code develop}; các chỉ số khác chỉ được coi là có dữ liệu khi stats-service đã
 * nhận ít nhất một sự kiện nguồn (xem {@link MetricSource}).
 */
public enum Metric {
    NEW_USERS("newUsers", "AccountRegistered", false),
    NEW_MENTORS("newMentors", "MentorApproved", false),
    BOOKINGS_CREATED("bookingsCreated", "BookingCreated", false),
    BOOKINGS_CONFIRMED("bookingsConfirmed", "BookingConfirmed", true),
    BOOKINGS_CANCELLED("bookingsCancelled", "BookingCancelled", true),
    BOOKINGS_REJECTED("bookingsRejected", "BookingRejected", true),
    BOOKINGS_CONFIRMATION_FAILED("bookingsConfirmationFailed", "BookingConfirmationFailed", true),
    BOOKINGS_COMPLETED("bookingsCompleted", "BookingCompleted", false),
    PAYMENTS_SUCCEEDED("paymentsSucceeded", "PaymentSucceeded", true),
    PAYMENTS_FAILED("paymentsFailed", "PaymentFailed", true),
    PAYMENTS_REFUNDED("paymentsRefunded", "PaymentRefunded", true);

    private final String field;
    private final String eventType;
    private final boolean alwaysPublished;

    Metric(String field, String eventType, boolean alwaysPublished) {
        this.field = field;
        this.eventType = eventType;
        this.alwaysPublished = alwaysPublished;
    }

    /** Tên field đếm trong document {@code daily_stats}. */
    public String field() { return field; }
    public String eventType() { return eventType; }
    public boolean alwaysPublished() { return alwaysPublished; }

    public static Metric forEventType(String eventType) {
        for (Metric metric : values()) {
            if (metric.eventType.equals(eventType)) {
                return metric;
            }
        }
        return null;
    }

}
