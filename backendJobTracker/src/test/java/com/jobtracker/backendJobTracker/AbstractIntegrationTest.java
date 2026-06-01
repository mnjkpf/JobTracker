package com.jobtracker.backendJobTracker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.backendJobTracker.auth.CustomUserDetails;
import com.jobtracker.backendJobTracker.user.Role;
import com.jobtracker.backendJobTracker.user.User;
import com.jobtracker.backendJobTracker.user.UserRepository;

/**
 * Базовий клас для всіх integration-тестів.
 * <p>
 * Стартує повний Spring контекст + реальні Postgres та Redis у Docker через
 * {@link TestcontainersConfiguration} (@ServiceConnection). Кожен тест виконується
 * у транзакції, яка відкочується після завершення — повна ізоляція без ручного cleanup.
 * <p>
 * <b>Передумова запуску:</b> доступний Docker daemon (Testcontainers).
 * <p>
 * Автентифікація в тестах робиться через spring-security-test
 * ({@code .with(user(principal(...)))}), а не через реальний JWT-флоу — це ізолює
 * тести контролерів від механіки токенів.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    /** Створює і зберігає активного юзера з BCrypt-паролем. */
    protected User persistUser(String email, String rawPassword) {
        User user = new User();
        user.setEmail(email.toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setDisplayName("Test " + email);
        user.setRole(Role.USER);
        user.setActive(true);
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    /** Security principal для {@code .with(user(...))} на захищених запитах. */
    protected CustomUserDetails principal(User user) {
        return new CustomUserDetails(user);
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
