package com.mentormatching.gateway.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

@RestControllerAdvice
public class GatewayUnavailableExceptionHandler {

    @ExceptionHandler(ResourceAccessException.class)
    ProblemDetail downstreamUnavailable(ResourceAccessException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "The requested service is temporarily unavailable.");
        problem.setTitle("Downstream service unavailable");
        problem.setProperty("code", "DOWNSTREAM_UNAVAILABLE");
        return problem;
    }
}
