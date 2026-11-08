package com.mentormatching.booking.exception;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.UUID;

/**
 * Creates standardized ProblemDetail RFC 7807 responses.
 */
@Component
public class ProblemResponses {
    public static final String REQUEST_ID_ATTRIBUTE = ProblemResponses.class.getName() + ".requestId";
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    private final ObjectMapper mapper;

    public ProblemResponses(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public static String requestId(HttpServletRequest request) {
        Object existing = request.getAttribute(REQUEST_ID_ATTRIBUTE);
        if (existing instanceof String id) {
            return id;
        }
        String id = UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ID_ATTRIBUTE, id);
        return id;
    }

    public ProblemDetail create(HttpStatusCode status, String code, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:mentor-matching:problem:" + code.toLowerCase(Locale.ROOT).replace('_', '-')));
        Object original = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String path = original instanceof String value ? value : request.getRequestURI();
        try {
            problem.setInstance(URI.create(path));
        } catch (IllegalArgumentException ignored) {
            problem.setInstance(URI.create("urn:request:" + requestId(request)));
        }
        problem.setProperty("code", code);
        problem.setProperty("requestId", requestId(request));
        return problem;
    }

    public ProblemDetail standard(HttpStatusCode status, HttpServletRequest request) {
        String code = switch (status.value()) {
            case 400 -> "INVALID_REQUEST";
            case 401 -> "AUTHENTICATION_REQUIRED";
            case 403 -> "ACCESS_DENIED";
            case 404 -> "RESOURCE_NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 406 -> "NOT_ACCEPTABLE";
            case 409 -> "CONFLICT";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            case 429 -> "TOO_MANY_REQUESTS";
            case 503 -> "SERVICE_UNAVAILABLE";
            default -> status.is5xxServerError() ? "INTERNAL_SERVER_ERROR" : "REQUEST_REJECTED";
        };
        String detail = switch (status.value()) {
            case 400 -> "The request is invalid.";
            case 401 -> "Authentication is required to access this resource.";
            case 403 -> "You do not have permission to perform this request.";
            case 404 -> "The requested resource was not found.";
            case 405 -> "The HTTP method is not supported for this resource.";
            case 406 -> "The requested response format is not supported.";
            case 409 -> "The request conflicts with the current resource state.";
            case 415 -> "The request content type is not supported.";
            case 429 -> "Too many requests. Please try again later.";
            case 503 -> "The service is temporarily unavailable.";
            default -> status.is5xxServerError() ? "An unexpected error occurred." : "The request could not be processed.";
        };
        return create(status, code, detail, request);
    }

    public void write(HttpStatusCode status, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader(REQUEST_ID_HEADER, requestId(request));
        mapper.writeValue(response.getOutputStream(), standard(status, request));
    }
}
