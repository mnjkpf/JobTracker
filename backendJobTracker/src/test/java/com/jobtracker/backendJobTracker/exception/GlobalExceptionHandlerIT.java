package com.jobtracker.backendJobTracker.exception;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.jobtracker.backendJobTracker.AbstractIntegrationTest;

/**
 * IT для GlobalExceptionHandler: перевіряє, що @Valid помилки та malformed JSON
 * повертають 400, а не 500 з catch-all handleUnexpectedException.
 */
class GlobalExceptionHandlerIT extends AbstractIntegrationTest {

    private static final String REGISTER = "/api/v1/auth/register";

    @Test
    @DisplayName("register з невалідним body -> 400 з деталями полів")
    void validationError_returns400WithFieldDetails() throws Exception {
        mockMvc.perform(post(REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"\",\"displayName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.detail").value(containsString(":")));
    }

    @Test
    @DisplayName("register з malformed JSON -> 400 generic message")
    void malformedJson_returns400() throws Exception {
        mockMvc.perform(post(REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.detail").value("Request body is malformed or missing"));
    }
}
