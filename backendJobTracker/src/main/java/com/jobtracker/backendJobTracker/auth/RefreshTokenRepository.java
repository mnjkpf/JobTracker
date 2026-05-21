package com.jobtracker.backendJobTracker.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID>{
    RefreshToken findByTokenHash(String tokenHash);

    List<RefreshToken> findByUser_Id(UUID userId);

    // Знайти всі активні токени юзера (не revoked)
    List<RefreshToken> findByUser_IdAndRevokedFalse(UUID userId);

        @Modifying
        @Query("""
                UPDATE RefreshToken t
                SET t.revoked = true
                WHERE t.user.id = :userId
                AND t.revoked = false
                """)
        int revokeAllForUser(@Param("userId") UUID userId);


}
