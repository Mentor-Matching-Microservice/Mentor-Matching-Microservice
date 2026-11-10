package com.mentormatching.notification.event;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Định dạng nội dung thông báo tiếng Việt dùng chung cho các mapper. */
final class Texts {

    static final int MAX_REASON_LENGTH = 300;

    private Texts() {
    }

    /** Lý do do service khác gửi: cắt độ dài để thông báo không quá dài; null nếu không có. */
    static String reason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        String trimmed = reason.strip();
        return trimmed.length() <= MAX_REASON_LENGTH ? trimmed : trimmed.substring(0, MAX_REASON_LENGTH - 1) + "…";
    }

    static String withReason(String reason) {
        return reason == null ? "" : ". Lý do: " + reason;
    }

    /** {@code 500000, VND} → {@code 500.000 VND}; {@code 12.5, USD} → {@code 12,5 USD}. */
    static String money(BigDecimal amount, String currency) {
        if (amount == null) {
            return null;
        }
        DecimalFormat format = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("vi-VN")));
        String unit = currency == null || currency.isBlank() ? "" : " " + currency.strip().toUpperCase(Locale.ROOT);
        return format.format(amount) + unit;
    }
}
