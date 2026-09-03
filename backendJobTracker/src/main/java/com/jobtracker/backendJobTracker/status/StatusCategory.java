package com.jobtracker.backendJobTracker.status;

import java.time.Instant;
import java.util.UUID;

import com.jobtracker.backendJobTracker.application.enums.ApplicationStatus;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Per-user, customizable board column. Replaces the fixed ApplicationStatus enum
 * on Application. The old enum is reused here only as an optional "semantic type"
 * ({@link #systemType}) that drives special behaviour (applied date, interview-prep
 * automation, statistics) — custom user statuses have systemType == null.
 */
@Entity
@Table(name = "status_categories",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_status_categories_user_name", columnNames = {"user_id", "name"}))
@Getter
@Setter
public class StatusCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 20)
    private String color; // hex, e.g. "#3b82f6"

    @Column(nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_type")
    private ApplicationStatus systemType; // nullable → custom status

    @Column(name = "is_terminal", nullable = false)
    private boolean terminal = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
