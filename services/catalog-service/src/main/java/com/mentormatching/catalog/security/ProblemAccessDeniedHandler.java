package com.mentormatching.catalog.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import com.mentormatching.catalog.exception.ProblemResponses;

@Component
public class ProblemAccessDeniedHandler implements AccessDeniedHandler {

    private final ProblemResponses problems;

    public ProblemAccessDeniedHandler(ProblemResponses problems) {
        this.problems = problems;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        problems.write(HttpStatus.FORBIDDEN, request, response);
    }
}
