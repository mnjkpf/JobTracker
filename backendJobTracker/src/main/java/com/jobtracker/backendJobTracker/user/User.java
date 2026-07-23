package com.jobtracker.backendJobTracker.user;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
 
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    /** Login identifier. Must be unique. Lowercased on save (handled in AuthService). */
    @Column(unique = true, nullable = false)
    private String email;
 
    /** BCrypt hash. Never stores raw password. */
    @Column(nullable = false)
    private String passwordHash;
 
    /**
     * Optional display name shown in UI greetings, comments, etc.
     * Independent of any name fields in MasterCV — user might want to
     * appear as "Nikum" in app while their CV says "Mykyta Romanchenko".
     */
    private String displayName;
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
 
    /** Soft-disable account without deleting. Admin can flip to block login. */
    @Column(nullable = false)
    private boolean isActive = true;
 
    /** Set after user clicks email verification link. */
    @Column(nullable = false)
    private boolean isEmailVerified = false;
 
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
 
    @Column(nullable = false)
    private Instant updatedAt;
 
    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }
 
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
 
    // Майбутні зв'язки (додаються в інших тижнях):
    //   @OneToOne MasterCV masterCv     — Тиждень 5 (CV з name, surname, country, etc.)
    //   @OneToMany List<Application>    — Тиждень 3 (всі заявки юзера)
    //   @OneToMany List<RefreshToken>   — Тиждень 2 (auth sessions)
    //   @OneToMany List<InterviewPrep>  — Тиждень 8
}

