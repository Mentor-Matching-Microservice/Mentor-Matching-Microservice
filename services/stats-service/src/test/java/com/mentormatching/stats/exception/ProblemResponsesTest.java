package com.mentormatching.stats.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ProblemResponsesTest {

    // Giống JsonMapper của ứng dụng: Spring Boot đăng ký ProblemDetailJacksonMixin để làm phẳng properties.
    private final JsonMapper mapper = JsonMapper.builder()
            .addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class)
            .build();
    private final ProblemResponses problems = new ProblemResponses(mapper);

    @ParameterizedTest
    @CsvSource({
            "400,INVALID_REQUEST", "401,AUTHENTICATION_REQUIRED", "403,ACCESS_DENIED",
            "404,RESOURCE_NOT_FOUND", "405,METHOD_NOT_ALLOWED", "409,CONFLICT",
            "415,UNSUPPORTED_MEDIA_TYPE", "429,TOO_MANY_REQUESTS", "503,SERVICE_UNAVAILABLE",
            "500,INTERNAL_SERVER_ERROR", "502,INTERNAL_SERVER_ERROR", "418,REQUEST_REJECTED"})
    void mapsStatusToStableCode(int status, String code) {
        ProblemDetail problem = problems.standard(HttpStatusCode.valueOf(status), request("/api/v1/notifications"));
        assertThat(problem.getStatus()).isEqualTo(status);
        assertThat(problem.getProperties()).containsEntry("code", code);
        assertThat(problem.getType()).isEqualTo(
                URI.create("urn:mentor-matching:problem:" + code.toLowerCase().replace('_', '-')));
    }

    @Test
    void usesOriginalUriOfErrorDispatchAsInstance() {
        MockHttpServletRequest request = request("/error");
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/api/v1/notifications/abc");
        assertThat(problems.standard(HttpStatus.NOT_FOUND, request).getInstance())
                .isEqualTo(URI.create("/api/v1/notifications/abc"));
    }

    @Test
    void fallsBackToRequestUrnWhenPathIsNotAUri() {
        MockHttpServletRequest request = request("/bad path with spaces");
        ProblemDetail problem = problems.standard(HttpStatus.BAD_REQUEST, request);
        assertThat(problem.getInstance().toString()).isEqualTo("urn:request:" + ProblemResponses.requestId(request));
    }

    @Test
    void requestIdIsServerGeneratedAndStablePerRequest() {
        MockHttpServletRequest request = request("/x");
        request.addHeader(ProblemResponses.REQUEST_ID_HEADER, "client-supplied");
        String first = ProblemResponses.requestId(request);
        assertThat(first).isNotEqualTo("client-supplied").hasSize(36);
        assertThat(ProblemResponses.requestId(request)).isEqualTo(first);
    }

    @Test
    void writeProducesProblemJsonAndKeepsExistingHeaders() throws Exception {
        MockHttpServletRequest request = request("/api/v1/notifications");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setHeader("WWW-Authenticate", "Bearer");

        problems.write(HttpStatus.UNAUTHORIZED, request, response);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/problem+json");
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        JsonNode body = mapper.readTree(response.getContentAsString());
        assertThat(body.get("code").asString()).isEqualTo("AUTHENTICATION_REQUIRED");
        assertThat(body.get("requestId").asString()).isEqualTo(response.getHeader(ProblemResponses.REQUEST_ID_HEADER));
    }

    @Test
    void writeDoesNothingWhenResponseIsCommitted() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setCommitted(true);
        problems.write(HttpStatus.UNAUTHORIZED, request("/x"), response);
        assertThat(response.getContentAsString()).isEmpty();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void apiExceptionOnlyAcceptsClientErrorsWithStableCodes() {
        assertThat(new ApiException(HttpStatus.NOT_FOUND, "INVALID_DATE_RANGE", "Invalid date range.").getCode())
                .isEqualTo("INVALID_DATE_RANGE");
        assertThatThrownBy(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "BOOM", "x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApiException(HttpStatus.BAD_REQUEST, "lower-case", "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static MockHttpServletRequest request(String uri) {
        return new MockHttpServletRequest("GET", uri);
    }
}
