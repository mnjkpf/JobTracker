package com.jobtracker.backendJobTracker.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.jobtracker.backendJobTracker.AbstractIntegrationTest;

/**
 * IT для AuthController: register / login / logout + помилкові сценарії.
 * <p>Потребує Docker (Testcontainers Postgres + Redis).
 * <p>
 * <b>Увага щодо поточних блокерів</b> (див. звіт):
 * <ul>
 *   <li>login порівнює SHA-256(пароль) з BCrypt-хешем — happy-path login наразі впаде 401
 *       поки AuthService не почне використовувати PasswordEncoder.matches(...);</li>
 *   <li>endpoint POST /api/v1/auth/refresh у контролері відсутній, тож rotation
 *       через HTTP тут не тестується (логіка покрита в JwtServiceTest).</li>
 * </ul>
 * Тести написані під ОЧІКУВАНУ поведінку — вони стають зеленими після фіксу багів.
 */
class AuthControllerIT extends AbstractIntegrationTest {

    private static final String REGISTER = "/api/v1/auth/register";
    private static final String LOGIN = "/api/v1/auth/login";
    private static final String LOGOUT = "/api/v1/auth/logout";

    private Map<String, Object> credentials(String email, String password) {
        return Map.of("email", email, "password", password, "displayName", "Tester");
    }

    @Test
    @DisplayName("register -> 201 + видає refreshToken та tokenType Bearer")
    void registerReturnsTokens() throws Exception {
        mockMvc.perform(post(REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(credentials("new@example.com", "Passw0rd!"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("register з існуючим email -> 409 Conflict")
    void duplicateEmailConflict() throws Exception {
        persistUser("dup@example.com", "Passw0rd!");

        mockMvc.perform(post(REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(credentials("dup@example.com", "Passw0rd!"))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("login з валідними креденшелами -> 200")
    void loginSucceeds() throws Exception {
        // Реєструємо через HTTP, щоб пароль зберігся тим самим способом, що очікує login.
        mockMvc.perform(post(REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(credentials("login@example.com", "Passw0rd!"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post(LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "login@example.com", "password", "Passw0rd!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("login з невірним паролем -> 401")
    void loginWrongPassword() throws Exception {
        mockMvc.perform(post(REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(credentials("wrongpass@example.com", "Passw0rd!"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post(LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "wrongpass@example.com", "password", "WRONG"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("logout з будь-яким (навіть невідомим) refresh token -> 204")
    void logoutIsIdempotent() throws Exception {
        mockMvc.perform(post(LOGOUT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", "unknown-raw-token"))))
                .andExpect(status().isNoContent());
    }
}
