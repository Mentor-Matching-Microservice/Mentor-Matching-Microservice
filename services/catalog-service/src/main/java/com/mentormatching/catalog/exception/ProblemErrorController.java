package com.mentormatching.catalog.exception;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProblemErrorController implements ErrorController {

    private final ProblemResponses problems;

    public ProblemErrorController(ProblemResponses problems) {
        this.problems = problems;
    }

    @RequestMapping("${server.error.path:/error}")
    public ResponseEntity<ProblemDetail> error(HttpServletRequest request) {
        Object value = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = value instanceof Integer number && number >= 400 && number <= 599 ? number : 500;
        HttpStatusCode code = HttpStatusCode.valueOf(status);
        return ResponseEntity.status(code).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .header(ProblemResponses.REQUEST_ID_HEADER, ProblemResponses.requestId(request))
                .body(problems.standard(code, request));
    }
}
