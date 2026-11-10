package com.mentormatching.booking.exception;

import com.mentormatching.booking.config.SecurityConfiguration;
import com.mentormatching.booking.security.ProblemAccessDeniedHandler;
import com.mentormatching.booking.security.ProblemAuthenticationEntryPoint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ProblemContractTest.Endpoints.class, ProblemErrorController.class})
@Import({ProblemContractTest.Endpoints.class, GlobalExceptionHandler.class, ProblemResponses.class, RequestIdFilter.class,
        SecurityConfiguration.class, ProblemAuthenticationEntryPoint.class, ProblemAccessDeniedHandler.class})
class ProblemContractTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @RestController
    static class Endpoints {
        record SamplePayload(@NotBlank @Email String email) {}

        @PostMapping("/test/validation")
        String validate(@Valid @RequestBody SamplePayload body) {
            return "ok";
        }

        @GetMapping("/test/conflict")
        String conflict() {
            throw new ApiException(HttpStatus.CONFLICT, "CONFLICT", "Resource conflict occurred.");
        }

        @GetMapping("/test/failure")
        String failure() {
            throw new IllegalStateException("Internal error occurred");
        }

        @GetMapping("/test/denied")
        String denied() {
            throw new AccessDeniedException("private permission details");
        }

        @GetMapping("/test/min")
        String minimum(@RequestParam @Min(1) int count) {
            return "ok";
        }
    }

    @Test
    void validationReturnsProblemDetail() throws Exception {
        mvc.perform(post("/test/validation").with(user("mentor")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invalid-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    void customApiExceptionReturnsProblemDetail() throws Exception {
        mvc.perform(get("/test/conflict").with(user("mentor")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("CONFLICT")))
                .andExpect(jsonPath("$.detail", is("Resource conflict occurred.")));
    }

    @Test
    void unexpectedErrorReturnsInternalServerErrorProblemDetail() throws Exception {
        mvc.perform(get("/test/failure").with(user("mentor")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code", is("INTERNAL_SERVER_ERROR")))
                .andExpect(jsonPath("$.detail", is("An unexpected error occurred.")));
    }

    @Test
    void unauthenticatedRequestReturnsUnauthorized() throws Exception {
        mvc.perform(get("/test/conflict"))
                .andExpect(status().isUnauthorized());
    }
}
