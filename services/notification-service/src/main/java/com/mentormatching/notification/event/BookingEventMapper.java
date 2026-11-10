package com.mentormatching.notification.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mentormatching.notification.client.BookingParticipants;
import com.mentormatching.notification.client.BookingParticipantsClient;
import com.mentormatching.notification.domain.NotificationType;
import org.springframework.stereotype.Component;

/**
 * Sự kiện vòng đời booking (routing key {@code booking.lifecycle}) → thông báo cho học viên và/hoặc mentor.
 *
 * <p>booking-service hiện chỉ gửi {@code bookingId} và {@code reason}, nên người nhận được tra qua API nội bộ
 * có sẵn của booking. Nếu sau này payload mang sẵn {@code studentUserId}/{@code mentorUserId} thì dùng luôn.
 *
 * <table>
 *   <tr><th>eventType</th><th>Học viên</th><th>Mentor</th></tr>
 *   <tr><td>BookingCreated</td><td></td><td>✔ yêu cầu đặt lịch mới</td></tr>
 *   <tr><td>BookingConfirmed</td><td>✔</td><td>✔</td></tr>
 *   <tr><td>BookingRejected</td><td>✔ (kèm lý do)</td><td></td></tr>
 *   <tr><td>BookingCancelled</td><td>✔ (kèm lý do)</td><td>✔ (kèm lý do)</td></tr>
 *   <tr><td>BookingConfirmationFailed</td><td>✔ (kèm lý do)</td><td></td></tr>
 *   <tr><td>BookingCompleted</td><td>✔ mời đánh giá</td><td>✔</td></tr>
 * </table>
 */
@Component
public class BookingEventMapper implements EventNotificationMapper {

    public static final String CREATED = "BookingCreated";
    public static final String CONFIRMED = "BookingConfirmed";
    public static final String REJECTED = "BookingRejected";
    public static final String CANCELLED = "BookingCancelled";
    public static final String CONFIRMATION_FAILED = "BookingConfirmationFailed";
    public static final String COMPLETED = "BookingCompleted";
    static final String REFERENCE_TYPE = "BOOKING";

    private final BookingParticipantsClient bookings;

    public BookingEventMapper(BookingParticipantsClient bookings) {
        this.bookings = bookings;
    }

    @Override
    public Set<String> eventTypes() {
        return Set.of(CREATED, CONFIRMED, REJECTED, CANCELLED, CONFIRMATION_FAILED, COMPLETED);
    }

    @Override
    public List<NotificationDraft> map(DomainEvent event) {
        String bookingId = event.requiredText("bookingId");
        BookingParticipants people = participants(event, bookingId);
        String reason = Texts.reason(event.optionalText("reason"));
        List<NotificationDraft> drafts = new ArrayList<>(2);
        switch (event.eventType()) {
            case CREATED -> drafts.add(draft(people.mentorUserId(), NotificationType.BOOKING_CREATED, bookingId,
                    "Yêu cầu đặt lịch mới",
                    "Bạn có một yêu cầu đặt lịch học mới (mã #" + bookingId + "). Vui lòng xem và phản hồi."));
            case CONFIRMED -> {
                drafts.add(draft(people.learnerUserId(), NotificationType.BOOKING_CONFIRMED, bookingId,
                        "Buổi học đã được xác nhận",
                        "Buổi học #" + bookingId + " đã được xác nhận. Hẹn gặp bạn đúng giờ!"));
                drafts.add(draft(people.mentorUserId(), NotificationType.BOOKING_CONFIRMED, bookingId,
                        "Có buổi học mới được xác nhận",
                        "Buổi học #" + bookingId + " đã được học viên thanh toán và xác nhận."));
            }
            case REJECTED -> drafts.add(draft(people.learnerUserId(), NotificationType.BOOKING_REJECTED, bookingId,
                    "Yêu cầu đặt lịch bị từ chối",
                    "Mentor đã từ chối yêu cầu đặt lịch #" + bookingId + Texts.withReason(reason)
                            + ". Bạn có thể chọn khung giờ hoặc mentor khác."));
            case CANCELLED -> {
                String message = "Buổi học #" + bookingId + " đã bị huỷ" + Texts.withReason(reason) + ".";
                drafts.add(draft(people.learnerUserId(), NotificationType.BOOKING_CANCELLED, bookingId,
                        "Buổi học đã bị huỷ", message));
                drafts.add(draft(people.mentorUserId(), NotificationType.BOOKING_CANCELLED, bookingId,
                        "Buổi học đã bị huỷ", message));
            }
            case CONFIRMATION_FAILED -> drafts.add(draft(people.learnerUserId(), NotificationType.BOOKING_CANCELLED,
                    bookingId, "Không thể xác nhận buổi học",
                    "Buổi học #" + bookingId + " chưa thể xác nhận" + Texts.withReason(reason)
                            + ". Nếu bạn đã thanh toán, khoản tiền sẽ được hoàn lại."));
            case COMPLETED -> {
                drafts.add(draft(people.learnerUserId(), NotificationType.BOOKING_COMPLETED, bookingId,
                        "Buổi học đã hoàn thành",
                        "Buổi học #" + bookingId + " đã hoàn thành. Hãy để lại đánh giá cho mentor nhé!"));
                drafts.add(draft(people.mentorUserId(), NotificationType.BOOKING_COMPLETED, bookingId,
                        "Buổi học đã hoàn thành", "Buổi học #" + bookingId + " đã hoàn thành."));
            }
            default -> throw new InvalidEventException("Unsupported booking event");
        }
        return drafts;
    }

    private BookingParticipants participants(DomainEvent event, String bookingId) {
        UUID learner = event.optionalUuid("studentUserId", "learnerUserId");
        UUID mentor = event.optionalUuid("mentorUserId");
        if (learner != null && mentor != null) {
            return new BookingParticipants(bookingId, learner, mentor);
        }
        return bookings.find(bookingId);
    }

    private static NotificationDraft draft(UUID recipient, NotificationType type, String bookingId, String title,
                                           String message) {
        return new NotificationDraft(recipient, type, title, message, REFERENCE_TYPE, bookingId);
    }
}
