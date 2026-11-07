package com.mentormatching.booking.exception;

import java.net.URI;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;

/** Maps API failures to the RFC 7807 Problem Details response format. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatus(
            ResponseStatusException exception, HttpServletRequest request) {
        HttpStatusCode status = exception.getStatusCode();
        String code = statusCode(status);
        String detail = exception.getReason() == null ? defaultDetail(status) : exception.getReason();
        return ResponseEntity.status(status).body(problem(status, code, detail, request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(
            Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                        "An unexpected error occurred.", request));
    }

    private static ProblemDetail problem(HttpStatusCode status, String code, String detail,
            HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:mentor-matching:problem:" + code.toLowerCase(Locale.ROOT).replace('_', '-')));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        return problem;
    }

    private static String statusCode(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "INVALID_REQUEST";
            case 401 -> "AUTHENTICATION_REQUIRED";
            case 403 -> "ACCESS_DENIED";
            case 404 -> "RESOURCE_NOT_FOUND";
            case 409 -> "CONFLICT";
            default -> status.is5xxServerError() ? "INTERNAL_SERVER_ERROR" : "REQUEST_REJECTED";
        };
    }

    private static String defaultDetail(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "The request is invalid.";
            case 401 -> "Authentication is required to access this resource.";
            case 403 -> "You do not have permission to perform this request.";
            case 404 -> "The requested resource was not found.";
            case 409 -> "The request conflicts with the current resource state.";
            default -> status.is5xxServerError() ? "An unexpected error occurred."
                    : "The request could not be processed.";
        };
    }
}
