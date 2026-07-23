package com.jobtracker.backendJobTracker.application;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.jobtracker.backendJobTracker.AbstractIntegrationTest;
import com.jobtracker.backendJobTracker.auth.CustomUserDetails;
import com.jobtracker.backendJobTracker.user.User;

/**
 * Найважливіший IT: повний життєвий цикл заявки через HTTP + multi-tenancy.
 * Автентифікація — через spring-security-test principal (CustomUserDetails).
 * <p>Потребує Docker (Testcontainers Postgres + Redis).
 */
class ApplicationControllerIT extends AbstractIntegrationTest {

    private static final String BASE = "/api/v1/applications";

    private Map<String, Object> validCreateBody() {
        return Map.of(
                "name", "Backend Engineer",
                "companyName", "Acme",
                "url", "https://example.com/job/1",
                "contractType", "B2B",
                "seniority", "MID",
                "workMode", "REMOTE");
    }

    private String createApplication(CustomUserDetails principal, Map<String, Object> body) throws Exception {
        MvcResult result = mockMvc.perform(post(BASE)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    @DisplayName("Повний цикл: create -> GET -> PATCH details -> PATCH status -> history(2)")
    void fullLifecycle() throws Exception {
        User user = persistUser("owner@example.com", "Passw0rd!");
        CustomUserDetails principal = principal(user);

        String id = createApplication(principal, validCreateBody());

        // GET повертає створену заявку.
        mockMvc.perform(get(BASE + "/{id}", id).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Backend Engineer"))
                .andExpect(jsonPath("$.companyName").value("Acme"));

        // PATCH деталей.
        mockMvc.perform(patch(BASE + "/{id}", id)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Senior Backend Engineer"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Senior Backend Engineer"));

        // PATCH статусу SAVED -> APPLIED.
        mockMvc.perform(patch(BASE + "/{id}/status", id)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "APPLIED", "note", "Sent CV"))))
                .andExpect(status().isOk());

        // History має 2 записи: (null->SAVED) при створенні + (SAVED->APPLIED).
        mockMvc.perform(get(BASE + "/{id}/status-history", id).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].toStatus").value("SAVED"))
                .andExpect(jsonPath("$[1].fromStatus").value("SAVED"))
                .andExpect(jsonPath("$[1].toStatus").value("APPLIED"));
    }

    @Test
    @DisplayName("Недозволений перехід SAVED -> OFFER повертає 422")
    void invalidTransitionReturns422() throws Exception {
        User user = persistUser("owner2@example.com", "Passw0rd!");
        CustomUserDetails principal = principal(user);
        String id = createApplication(principal, validCreateBody());

        mockMvc.perform(patch(BASE + "/{id}/status", id)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "OFFER"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("Multi-tenancy: юзер B не бачить заявку юзера A (404)")
    void tenantIsolation() throws Exception {
        User userA = persistUser("a@example.com", "Passw0rd!");
        User userB = persistUser("b@example.com", "Passw0rd!");
        String idA = createApplication(principal(userA), validCreateBody());

        mockMvc.perform(get(BASE + "/{id}", idA).with(user(principal(userB))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Без автентифікації -> 401")
    void unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get(BASE + "/{id}", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isUnauthorized());
    }
}
