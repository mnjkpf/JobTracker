package com.jobtracker.backendJobTracker.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.jobtracker.backendJobTracker.AbstractIntegrationTest;
import com.jobtracker.backendJobTracker.auth.CustomUserDetails;
import com.jobtracker.backendJobTracker.user.User;

/**
 * IT для CompanyController: CRUD + findOrCreate дедуплікація + tenant isolation.
 * <p>Потребує Docker (Testcontainers Postgres + Redis).
 */
class CompanyControllerIT extends AbstractIntegrationTest {

    private static final String BASE = "/api/v1/companies";

    @Autowired
    private CompanyService companyService;

    private String createCompany(CustomUserDetails principal, String name) throws Exception {
        MvcResult result = mockMvc.perform(post(BASE)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", name, "industry", "IT", "size", "STARTUP"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    @DisplayName("CRUD: create -> GET -> DELETE")
    void crud() throws Exception {
        CustomUserDetails principal = principal(persistUser("c-crud@example.com", "Passw0rd!"));
        String id = createCompany(principal, "Allegro");

        mockMvc.perform(get(BASE + "/{id}", id).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Allegro"));

        mockMvc.perform(delete(BASE + "/{id}", id).with(user(principal)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE + "/{id}", id).with(user(principal)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Дублікат назви для того ж юзера -> 409")
    void duplicateNameConflict() throws Exception {
        CustomUserDetails principal = principal(persistUser("c-dup@example.com", "Passw0rd!"));
        createCompany(principal, "Comarch");

        mockMvc.perform(post(BASE)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Comarch"))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("findOrCreate дедуплікує: двічі та сама назва -> одна компанія")
    void findOrCreateDeduplicates() {
        User user = persistUser("c-foc@example.com", "Passw0rd!");
        Company first = companyService.findOrCreate(user.getId(), "Google");
        Company second = companyService.findOrCreate(user.getId(), "Google");

        assertThat(second.getId()).isEqualTo(first.getId());
    }

    @Test
    @DisplayName("Tenant isolation: юзер B не бачить компанію юзера A (404)")
    void tenantIsolation() throws Exception {
        CustomUserDetails a = principal(persistUser("c-a@example.com", "Passw0rd!"));
        CustomUserDetails b = principal(persistUser("c-b@example.com", "Passw0rd!"));
        String idA = createCompany(a, "PrivateCorp");

        mockMvc.perform(get(BASE + "/{id}", idA).with(user(b)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Невідома компанія -> 404")
    void unknownCompany() throws Exception {
        CustomUserDetails principal = principal(persistUser("c-404@example.com", "Passw0rd!"));
        mockMvc.perform(get(BASE + "/{id}", UUID.randomUUID()).with(user(principal)))
                .andExpect(status().isNotFound());
    }
}
