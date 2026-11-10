package com.mentormatching.notification.event;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mentormatching.notification.client.BookingParticipantsClient;
import com.mentormatching.notification.domain.NotificationType;
import org.springframework.stereotype.Component;

/**
 * {@code ReviewCreated} (routing key {@code review.events}) → thông báo cho mentor được đánh giá.
 * review-service gửi {@code mentorId} là id hồ sơ mentor (không phải tài khoản), nên tài khoản mentor được tra
 * từ {@code bookingId} qua API nội bộ của booking; nếu payload có sẵn {@code mentorUserId} thì dùng luôn.
 */
@Component
public class ReviewEventMapper implements EventNotificationMapper {

    public static final String REVIEW_CREATED = "ReviewCreated";

    private final BookingParticipantsClient bookings;

    public ReviewEventMapper(BookingParticipantsClient bookings) {
        this.bookings = bookings;
    }

    @Override
    public Set<String> eventTypes() {
        return Set.of(REVIEW_CREATED);
    }

    @Override
    public List<NotificationDraft> map(DomainEvent event) {
        UUID mentor = event.optionalUuid("mentorUserId");
        if (mentor == null) {
            mentor = bookings.find(event.requiredText("bookingId")).mentorUserId();
        }
        String rating = event.optionalText("rating");
        String stars = rating != null && rating.matches("[1-5]") ? " " + rating + "★" : "";
        String reviewId = event.optionalText("reviewId");
        return List.of(new NotificationDraft(mentor, NotificationType.REVIEW_CREATED, "Bạn có đánh giá mới",
                "Học viên vừa để lại đánh giá" + stars + " cho buổi học của bạn.", "REVIEW",
                reviewId != null ? reviewId : event.optionalText("bookingId")));
    }
}
