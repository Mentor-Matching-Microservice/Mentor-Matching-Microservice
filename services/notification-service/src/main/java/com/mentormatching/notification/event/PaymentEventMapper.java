package com.mentormatching.notification.event;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mentormatching.notification.domain.NotificationType;
import org.springframework.stereotype.Component;

/**
 * Sự kiện thanh toán (routing key {@code payment.events}) → thông báo cho học viên đã thanh toán.
 * payment-service gửi sẵn {@code payerUserId}, {@code amount}, {@code currency}, {@code refundedAmount}, {@code reason}.
 */
@Component
public class PaymentEventMapper implements EventNotificationMapper {

    public static final String SUCCEEDED = "PaymentSucceeded";
    public static final String FAILED = "PaymentFailed";
    public static final String REFUNDED = "PaymentRefunded";
    static final String REFERENCE_TYPE = "PAYMENT";

    @Override
    public Set<String> eventTypes() {
        return Set.of(SUCCEEDED, FAILED, REFUNDED);
    }

    @Override
    public List<NotificationDraft> map(DomainEvent event) {
        UUID payer = event.requiredUuid("payerUserId", "payerId");
        String paymentId = event.optionalText("paymentId");
        String reference = paymentId != null ? paymentId : event.optionalText("bookingId");
        String currency = event.optionalText("currency");
        String reason = Texts.reason(event.optionalText("reason"));
        NotificationDraft draft = switch (event.eventType()) {
            case SUCCEEDED -> new NotificationDraft(payer, NotificationType.PAYMENT_SUCCESS, "Thanh toán thành công",
                    "Bạn đã thanh toán thành công" + amount(Texts.money(event.optionalDecimal("amount"), currency))
                            + ". Buổi học sẽ được xác nhận trong giây lát.", REFERENCE_TYPE, reference);
            case FAILED -> new NotificationDraft(payer, NotificationType.PAYMENT_FAILED, "Thanh toán không thành công",
                    "Thanh toán của bạn chưa thành công" + Texts.withReason(reason)
                            + ". Vui lòng thử lại trước khi hết thời gian giữ chỗ.", REFERENCE_TYPE, reference);
            case REFUNDED -> {
                String refunded = Texts.money(event.optionalDecimal("refundedAmount"), currency);
                if (refunded == null) {
                    refunded = Texts.money(event.optionalDecimal("amount"), currency);
                }
                yield new NotificationDraft(payer, NotificationType.PAYMENT_REFUNDED, "Đã hoàn tiền",
                        "Bạn đã được hoàn" + amount(refunded) + Texts.withReason(reason) + ".", REFERENCE_TYPE,
                        reference);
            }
            default -> throw new InvalidEventException("Unsupported payment event");
        };
        return List.of(draft);
    }

    private static String amount(String money) {
        return money == null ? "" : " " + money;
    }
}
