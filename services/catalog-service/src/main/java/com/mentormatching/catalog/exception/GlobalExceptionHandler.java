package com.mentormatching.catalog.exception;

import java.util.List;

import com.mentormatching.catalog.dto.response.FieldViolation;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ProblemResponses problems;

    public GlobalExceptionHandler(ProblemResponses problems) {
        this.problems = problems;
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, WebRequest webRequest) {
        HttpHeaders headers = ex.getHeaders();
        HttpStatusCode status = ex.getStatusCode();
        ProblemDetail problem = problems.create(status, "VALIDATION_ERROR", "One or more fields are invalid.",
                ((ServletWebRequest) webRequest).getRequest());
        List<FieldViolation> errors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> FieldViolation.builder()
                        .field(error instanceof org.springframework.validation.FieldError field ? field.getField() : "request")
                        .message(safeValidationMessage(error.getCode())).build())
                .distinct().toList();
        problem.setProperty("errors", errors);
        return finishResponse(ex, problem, headers, status, webRequest);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex, WebRequest webRequest) {
        HttpHeaders headers = ex.getHeaders();
        HttpStatusCode status = ex.getStatusCode();
        // Return-value validation is a server error, never a client validation failure.
        if (!status.is4xxClientError()) {
            return finishResponse(ex, null, headers, status, webRequest);
        }
        ProblemDetail problem = problems.create(status, "VALIDATION_ERROR", "One or more request parameters are invalid.",
                ((ServletWebRequest) webRequest).getRequest());
        problem.setProperty("errors", ex.getParameterValidationResults().stream()
                .map(result -> {
                    String name = result.getMethodParameter().getParameterName();
                    return FieldViolation.builder().field(name != null ? name : "arg" + result.getMethodParameter().getParameterIndex())
                            .message("Invalid value.").build();
                }).toList());
        return finishResponse(ex, problem, headers, status, webRequest);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Object> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        return finishResponse(ex, null, ex.getHeaders(), ex.getStatusCode(), request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Object> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex, WebRequest request) {
        return finishResponse(ex, null, ex.getHeaders(), ex.getStatusCode(), request);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Object> handleNotAcceptable(HttpMediaTypeNotAcceptableException ex, WebRequest request) {
        return finishResponse(ex, null, ex.getHeaders(), ex.getStatusCode(), request);
    }

    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<Object> handleRequestBinding(ServletRequestBindingException ex, WebRequest request) {
        return finishResponse(ex, null, ex.getHeaders(), ex.getStatusCode(), request);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Object> handleMissingPart(MissingServletRequestPartException ex, WebRequest request) {
        return finishResponse(ex, null, ex.getHeaders(), ex.getStatusCode(), request);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Object> handleNoHandler(NoHandlerFoundException ex, WebRequest request) {
        return finishResponse(ex, null, ex.getHeaders(), ex.getStatusCode(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Object> handleNoResource(NoResourceFoundException ex, WebRequest request) {
        return finishResponse(ex, null, ex.getHeaders(), ex.getStatusCode(), request);
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<Object> handleAsyncTimeout(AsyncRequestTimeoutException ex, WebRequest request) {
        return finishResponse(ex, null, ex.getHeaders(), ex.getStatusCode(), request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Object> handleUploadTooLarge(MaxUploadSizeExceededException ex, WebRequest request) {
        return finishResponse(ex, null, ex.getHeaders(), ex.getStatusCode(), request);
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<Object> handleErrorResponse(ErrorResponseException ex, WebRequest request) {
        return finishResponse(ex, null, ex.getHeaders(), ex.getStatusCode(), request);
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, WebRequest request) {
        return finishResponse(ex, null, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(ConversionNotSupportedException.class)
    public ResponseEntity<Object> handleConversionFailure(ConversionNotSupportedException ex, WebRequest request) {
        return finishResponse(ex, null, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleUnreadableMessage(HttpMessageNotReadableException ex, WebRequest request) {
        return finishResponse(ex, null, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(HttpMessageNotWritableException.class)
    public ResponseEntity<Object> handleUnwritableMessage(HttpMessageNotWritableException ex, WebRequest request) {
        return finishResponse(ex, null, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(MethodValidationException.class)
    public ResponseEntity<Object> handleMethodValidation(MethodValidationException ex, WebRequest request) {
        return finishResponse(ex, null, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public ResponseEntity<Object> handleUnusableAsyncResponse(AsyncRequestNotUsableException ex) {
        return null;
    }

    private ResponseEntity<Object> finishResponse(Exception ex, Object body, HttpHeaders headers,
                                                  HttpStatusCode status, WebRequest webRequest) {
        ServletWebRequest servlet = (ServletWebRequest) webRequest;
        if (servlet.getResponse() != null && servlet.getResponse().isCommitted()) {
            return null;
        }
        HttpServletRequest request = servlet.getRequest();
        ProblemDetail problem = body instanceof ProblemDetail detail ? detail : problems.standard(status, request);
        if (status.is5xxServerError()) {
            log.error("Request failed requestId={}", ProblemResponses.requestId(request), ex);
        }
        return response(problem, headers, status);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> handleBusiness(ApiException ex, HttpServletRequest request) {
        return response(problems.create(ex.getStatus(), ex.getCode(), ex.getMessage(), request),
                new HttpHeaders(), ex.getStatus());
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

    private String safeValidationMessage(String constraint) {
        if (constraint == null) {
            return "Invalid value.";
        }
        return switch (constraint) {
            case "NotBlank", "NotEmpty", "NotNull" -> "This field is required.";
            case "Email" -> "Must be a valid email address.";
            case "Size", "Length" -> "The length is outside the allowed range.";
            case "Min", "Max", "DecimalMin", "DecimalMax", "Positive", "PositiveOrZero", "Negative", "NegativeOrZero" ->
                    "The value is outside the allowed range.";
            default -> "Invalid value.";
        };
    }

    private ResponseEntity<Object> response(ProblemDetail problem, HttpHeaders original, HttpStatusCode status) {
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(original); // Keep framework headers such as Allow and Retry-After.
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        headers.set(ProblemResponses.REQUEST_ID_HEADER, (String) problem.getProperties().get("requestId"));
        return new ResponseEntity<>(problem, headers, status);
    }
}
