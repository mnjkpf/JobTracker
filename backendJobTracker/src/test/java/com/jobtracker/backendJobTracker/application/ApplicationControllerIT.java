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
import com.jobtracker.backendJobTracker.application.enums.ApplicationStatus;
import com.jobtracker.backendJobTracker.auth.CustomUserDetails;
import com.jobtracker.backendJobTracker.user.User;

/**
 * Full application lifecycle IT over HTTP + multi-tenancy.
 * <p>Requires Docker (Testcontainers Postgres + Redis).
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
    @DisplayName("Full cycle: create -> GET -> PATCH details -> PATCH status -> history(2)")
    void fullLifecycle() throws Exception {
        User user = persistUser("owner@example.com", "Passw0rd!");
        CustomUserDetails principal = principal(user);

        String id = createApplication(principal, validCreateBody());

        mockMvc.perform(get(BASE + "/{id}", id).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Backend Engineer"))
                .andExpect(jsonPath("$.companyName").value("Acme"))
                .andExpect(jsonPath("$.status.name").value("Saved"));

        mockMvc.perform(patch(BASE + "/{id}", id)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Senior Backend Engineer"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Senior Backend Engineer"));

        // Move Saved -> Applied by the user's Applied status id.
        String appliedId = statusId(user.getId(), ApplicationStatus.APPLIED).toString();
        mockMvc.perform(patch(BASE + "/{id}/status", id)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("statusId", appliedId, "note", "Sent CV"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.name").value("Applied"));

        mockMvc.perform(get(BASE + "/{id}/status-history", id).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].toLabel").value("Saved"))
                .andExpect(jsonPath("$[1].fromLabel").value("Saved"))
                .andExpect(jsonPath("$[1].toLabel").value("Applied"));
    }

    @Test
    @DisplayName("Free movement: Saved -> Offer is allowed (200)")
    void freeMovementAllowed() throws Exception {
        User user = persistUser("owner2@example.com", "Passw0rd!");
        CustomUserDetails principal = principal(user);
        String id = createApplication(principal, validCreateBody());

        String offerId = statusId(user.getId(), ApplicationStatus.OFFER).toString();
        mockMvc.perform(patch(BASE + "/{id}/status", id)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("statusId", offerId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.name").value("Offer"));
    }

    @Test
    @DisplayName("Multi-tenancy: user B cannot see user A's application (404)")
    void tenantIsolation() throws Exception {
        User userA = persistUser("a@example.com", "Passw0rd!");
        User userB = persistUser("b@example.com", "Passw0rd!");
        String idA = createApplication(principal(userA), validCreateBody());

        mockMvc.perform(get(BASE + "/{id}", idA).with(user(principal(userB))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Unauthenticated -> 401")
    void unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get(BASE + "/{id}", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isUnauthorized());
    }
}
