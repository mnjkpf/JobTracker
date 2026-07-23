package com.jobtracker.backendJobTracker.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, java.util.UUID> {
    Optional<VerificationToken> findByTokenHashAndTokenType(String tokenHash, VerificationTokenType tokenType);

    @Modifying
    @Query("""
            UPDATE VerificationToken vt
               SET vt.usedAt = :now
             WHERE vt.user.id = :userId
               AND vt.tokenType = :tokenType
               AND vt.usedAt IS NULL
            """)
    int invalidateAllForUser(@Param("userId") UUID userId,
                             @Param("tokenType") VerificationTokenType tokenType,
                             @Param("now") Instant now);


}
