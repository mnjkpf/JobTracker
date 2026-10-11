package com.jobtracker.backendJobTracker.user;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    List<User> findByRole(Role role);

    boolean existsByEmail(String email);

    /** Гостьові акаунти, що вже згоріли — вхід для cleanup-job. */
    List<User> findByIsGuestTrueAndGuestExpiresAtBefore(Instant cutoff);

}
