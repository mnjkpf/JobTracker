package com.jobtracker.backendJobTracker.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.jobtracker.backendJobTracker.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "verification_tokens")
@Getter
@Setter
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** SHA-256 хеш від raw токену. Raw ніколи не зберігається. */
    @Column(nullable = false, unique = true, name = "token_hash")
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "token_type")
    private VerificationTokenType tokenType;

    @Column(nullable = false, name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(nullable = false, name = "expires_at")
    private Instant expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    
    @Column(name = "used_at")
    private Instant usedAt;

    @PrePersist
    void onCreate() {
        
        Instant now = Instant.now();
        createdAt = now;
        if (expiresAt == null) {
            expiresAt = now.plus(Duration.ofHours(24));
        }
    }

    
    public boolean isUsable() {
        return usedAt == null && Instant.now().isBefore(expiresAt);
    }

    public void markUsed() {
        this.usedAt = Instant.now();
    }
}