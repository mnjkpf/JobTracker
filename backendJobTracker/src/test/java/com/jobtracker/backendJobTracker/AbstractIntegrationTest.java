package com.jobtracker.backendJobTracker;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.backendJobTracker.application.enums.ApplicationStatus;
import com.jobtracker.backendJobTracker.auth.CustomUserDetails;
import com.jobtracker.backendJobTracker.status.StatusCategoryRepository;
import com.jobtracker.backendJobTracker.status.StatusService;
import com.jobtracker.backendJobTracker.user.Role;
import com.jobtracker.backendJobTracker.user.User;
import com.jobtracker.backendJobTracker.user.UserRepository;

/**
 * Base class for integration tests. Full Spring context + real Postgres/Redis
 * (Testcontainers). Each test runs in a rolled-back transaction.
 * <p><b>Requires Docker.</b>
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

    @Autowired
    protected StatusService statusService;

    @Autowired
    protected StatusCategoryRepository statusCategoryRepository;

    /** Creates and saves an active user with BCrypt password AND the 9 default statuses. */
    protected User persistUser(String email, String rawPassword) {
        User user = new User();
        user.setEmail(email.toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setDisplayName("Test " + email);
        user.setRole(Role.USER);
        user.setActive(true);
        user.setEmailVerified(true);
        User saved = userRepository.save(user);
        statusService.seedDefaults(saved); // mirror AuthService.register
        return saved;
    }

    /** Resolve a seeded status id for a user by its semantic system type. */
    protected UUID statusId(UUID userId, ApplicationStatus systemType) {
        return statusCategoryRepository.findByUserIdAndSystemType(userId, systemType)
                .orElseThrow(() -> new IllegalStateException("No seeded status for " + systemType))
                .getId();
    }

    protected CustomUserDetails principal(User user) {
        return new CustomUserDetails(user);
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
