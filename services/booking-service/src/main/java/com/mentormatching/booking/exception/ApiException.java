package com.mentormatching.booking.exception;

import org.springframework.http.HttpStatus;

/**
 * Expected 4xx client errors.
 */
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String publicMessage) {
        super(publicMessage);
        if (status == null || !status.is4xxClientError()) {
            throw new IllegalArgumentException("ApiException requires a 4xx status");
        }
        if (code == null || !code.matches("[A-Z][A-Z0-9_]*")) {
            throw new IllegalArgumentException("A stable uppercase error code is required");
        }
        this.status = status;
        this.code = code;
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "ACCESS_DENIED", message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, "CONFLICT", message);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
