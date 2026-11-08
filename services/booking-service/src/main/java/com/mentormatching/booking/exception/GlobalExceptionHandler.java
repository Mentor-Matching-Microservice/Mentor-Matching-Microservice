package com.mentormatching.booking.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ProblemResponses problems;

    public GlobalExceptionHandler(ProblemResponses problems) {
        this.problems = problems;
    }

    public record FieldViolation(String field, String message) {}

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> handleApiException(ApiException ex, HttpServletRequest request) {
        ProblemDetail problem = problems.create(ex.getStatus(), ex.getCode(), ex.getMessage(), request);
        return response(problem, new HttpHeaders(), ex.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, WebRequest webRequest) {
        HttpHeaders headers = ex.getHeaders();
        HttpStatusCode status = ex.getStatusCode();
        ProblemDetail problem = problems.create(status, "VALIDATION_ERROR", "One or more fields are invalid.",
                ((ServletWebRequest) webRequest).getRequest());
        List<FieldViolation> errors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> new FieldViolation(
                        error instanceof org.springframework.validation.FieldError field ? field.getField() : "request",
                        safeValidationMessage(error.getCode())))
                .distinct().toList();
        problem.setProperty("errors", errors);
        return finishResponse(ex, problem, headers, status, webRequest);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex, WebRequest webRequest) {
        HttpHeaders headers = ex.getHeaders();
        HttpStatusCode status = ex.getStatusCode();
        if (!status.is4xxClientError()) {
            return finishResponse(ex, null, headers, status, webRequest);
        }
        ProblemDetail problem = problems.create(status, "VALIDATION_ERROR", "One or more request parameters are invalid.",
                ((ServletWebRequest) webRequest).getRequest());
        problem.setProperty("errors", ex.getParameterValidationResults().stream()
                .map(result -> {
                    String name = result.getMethodParameter().getParameterName();
                    return new FieldViolation(name != null ? name : "arg" + result.getMethodParameter().getParameterIndex(),
                            "Invalid value.");
                }).toList());
        return finishResponse(ex, problem, headers, status, webRequest);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, WebRequest webRequest) {
        return finishResponse(ex, null, ex.getHeaders(), ex.getStatusCode(), webRequest);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, WebRequest webRequest) {
        return finishResponse(ex, null, ex.getHeaders(), ex.getStatusCode(), webRequest);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex, WebRequest webRequest) {
        return finishResponse(ex, null, ex.getHeaders(), ex.getStatusCode(), webRequest);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, WebRequest webRequest) {
        return finishResponse(ex, null, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);
    }

    @ExceptionHandler(HttpMessageNotWritableException.class)
    public ResponseEntity<Object> handleHttpMessageNotWritable(HttpMessageNotWritableException ex, WebRequest webRequest) {
        return finishResponse(ex, null, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, webRequest);
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, WebRequest webRequest) {
        return finishResponse(ex, null, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);
    }

    @ExceptionHandler(ConversionNotSupportedException.class)
    public ResponseEntity<Object> handleConversionNotSupported(ConversionNotSupportedException ex, WebRequest webRequest) {
        return finishResponse(ex, null, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, webRequest);
    }

    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<Object> handleServletRequestBinding(ServletRequestBindingException ex, WebRequest webRequest) {
        return finishResponse(ex, null, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Object> handleMissingServletRequestPart(MissingServletRequestPartException ex, WebRequest webRequest) {
        return finishResponse(ex, null, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Object> handleNoHandlerFound(NoHandlerFoundException ex, WebRequest webRequest) {
        return finishResponse(ex, null, ex.getHeaders(), ex.getStatusCode(), webRequest);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Object> handleNoResourceFound(NoResourceFoundException ex, WebRequest webRequest) {
        return finishResponse(ex, null, ex.getHeaders(), ex.getStatusCode(), webRequest);
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<Object> handleAsyncRequestTimeout(AsyncRequestTimeoutException ex, WebRequest webRequest) {
        return finishResponse(ex, null, new HttpHeaders(), HttpStatus.SERVICE_UNAVAILABLE, webRequest);
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public ResponseEntity<Object> handleAsyncRequestNotUsable(AsyncRequestNotUsableException ex, WebRequest webRequest) {
        return null;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Object> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, WebRequest webRequest) {
        return finishResponse(ex, null, new HttpHeaders(), HttpStatus.PAYLOAD_TOO_LARGE, webRequest);
    }

    @ExceptionHandler(MethodValidationException.class)
    public ResponseEntity<Object> handleMethodValidation(MethodValidationException ex, WebRequest webRequest) {
        return finishResponse(ex, null, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<Object> handleErrorResponse(ErrorResponseException ex, WebRequest webRequest) {
        return finishResponse(ex, null, ex.getHeaders(), ex.getStatusCode(), webRequest);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        return response(problems.standard(HttpStatus.UNAUTHORIZED, request), headers, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return response(problems.standard(HttpStatus.FORBIDDEN, request), new HttpHeaders(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected failure requestId={}", ProblemResponses.requestId(request), ex);
        return response(problems.standard(HttpStatus.INTERNAL_SERVER_ERROR, request),
                new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Object> finishResponse(Exception ex, ProblemDetail body, HttpHeaders headers,
                                                  HttpStatusCode status, WebRequest webRequest) {
        HttpServletRequest request = ((ServletWebRequest) webRequest).getRequest();
        ProblemDetail problem = body != null ? body : problems.standard(status, request);
        return response(problem, headers, status);
    }

    private String safeValidationMessage(String constraint) {
        if (constraint == null) {
            return "Invalid value.";
        }
        return switch (constraint) {
            case "NotBlank", "NotEmpty", "NotNull" -> "This field is required.";
            case "Email" -> "Must be a valid email address.";
            case "Size", "Length" -> "The length is outside the allowed range.";
            case "Min", "Max", "DecimalMin", "DecimalMax", "Positive", "PositiveOrZero", "Negative", "NegativeOrZero" -> "The value is outside the allowed range.";
            default -> "Invalid value.";
        };
    }

    private ResponseEntity<Object> response(ProblemDetail problem, HttpHeaders original, HttpStatusCode status) {
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(original);
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        headers.set(ProblemResponses.REQUEST_ID_HEADER, (String) problem.getProperties().get("requestId"));
        return new ResponseEntity<>(problem, headers, status);
    }
}
