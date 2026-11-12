package com.mentormatching.notification.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Sự kiện nghiệp vụ nhận từ RabbitMQ theo định dạng phẳng mà booking/payment/review đang publish:
 * {@code {"eventId": "...", "eventType": "...", "occurredAt": "...", ...các trường riêng...}}.
 */
public record DomainEvent(String eventId, String eventType, Instant occurredAt, JsonNode payload) {

    public static final int MAX_ID_LENGTH = 100;

    public static DomainEvent parse(byte[] body, ObjectMapper mapper) {
        JsonNode root;
        try {
            root = mapper.readTree(body);
        } catch (JacksonException ex) {
            throw new InvalidEventException("Event body is not valid JSON");
        }
        if (root == null || !root.isObject()) {
            throw new InvalidEventException("Event body must be a JSON object");
        }
        String eventId = requiredEnvelopeField(root, "eventId");
        String eventType = requiredEnvelopeField(root, "eventType");
        if (eventId.length() > MAX_ID_LENGTH || eventType.length() > MAX_ID_LENGTH) {
            throw new InvalidEventException("eventId/eventType is too long");
        }
        Instant occurredAt = null;
        JsonNode occurred = root.get("occurredAt");
        if (occurred != null && occurred.isString()) {
            try {
                occurredAt = Instant.parse(occurred.asString());
            } catch (RuntimeException ignored) {
                // occurredAt chỉ để tham khảo; sai định dạng không làm hỏng sự kiện.
            }
        }
        return new DomainEvent(eventId, eventType, occurredAt, root);
    }

    /** UUID bắt buộc, chấp nhận nhiều tên trường (ví dụ {@code accountId} hoặc {@code userId}). */
    public UUID requiredUuid(String... fieldNames) {
        for (String name : fieldNames) {
            JsonNode value = payload.get(name);
            if (value != null && value.isString() && !value.asString().isBlank()) {
                try {
                    return UUID.fromString(value.asString().strip());
                } catch (IllegalArgumentException ex) {
                    throw new InvalidEventException(name + " must be a UUID");
                }
            }
        }
        throw new InvalidEventException("Missing required field " + String.join("|", fieldNames));
    }

    /** UUID tuỳ chọn; null nếu không có trường nào trong danh sách. Có nhưng sai định dạng thì là sự kiện lỗi. */
    public UUID optionalUuid(String... fieldNames) {
        for (String name : fieldNames) {
            JsonNode value = payload.get(name);
            if (value != null && value.isString() && !value.asString().isBlank()) {
                return requiredUuid(name);
            }
        }
        return null;
    }

    /** Giá trị bắt buộc dạng chuỗi hoặc số (ví dụ {@code bookingId} là {@code "123"} hoặc {@code 123}). */
    public String requiredText(String... fieldNames) {
        for (String name : fieldNames) {
            String value = optionalText(name);
            if (value != null) {
                return value;
            }
        }
        throw new InvalidEventException("Missing required field " + String.join("|", fieldNames));
    }

    /** Số thập phân tuỳ chọn (ví dụ số tiền); null nếu không có hoặc không phải số. */
    public BigDecimal optionalDecimal(String fieldName) {
        JsonNode value = payload.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return value.isNumber() ? value.decimalValue() : new BigDecimal(value.asString().strip());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /** Chuỗi tuỳ chọn, cắt khoảng trắng; null nếu không có hoặc rỗng. */
    public String optionalText(String fieldName) {
        JsonNode value = payload.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.isString() ? value.asString() : value.toString();
        return text.isBlank() ? null : text.strip();
    }

    private static String requiredEnvelopeField(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || !value.isString() || value.asString().isBlank()) {
            throw new InvalidEventException("Missing required field " + field);
        }
        return value.asString().strip();
    }
}
