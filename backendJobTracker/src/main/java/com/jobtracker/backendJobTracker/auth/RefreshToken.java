package com.jobtracker.backendJobTracker.auth;


import java.time.Instant;
import java.util.UUID;

import com.jobtracker.backendJobTracker.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, unique = true)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;


    @Column(nullable = false)
    private boolean revoked = false;
    
    @Column(nullable = false)
    private Instant createdAt;

        @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
 
    /** Convenience: a token is active if not revoked and not expired. */
    public boolean isActive() {
        return !revoked && Instant.now().isBefore(expiresAt);
    }

}
