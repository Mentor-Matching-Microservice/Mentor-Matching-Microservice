package com.mentormatching.catalog.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import com.mentormatching.catalog.exception.ProblemResponses;

@Component
public class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ProblemResponses problems;

    public ProblemAuthenticationEntryPoint(ProblemResponses problems) {
        this.problems = problems;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        problems.write(HttpStatus.UNAUTHORIZED, request, response);
    }
}
