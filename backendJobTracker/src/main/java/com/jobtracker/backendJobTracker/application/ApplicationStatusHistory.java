package com.jobtracker.backendJobTracker.application;

import java.time.Instant;
import java.util.UUID;

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

/**
 * Append-only audit log for status transitions. Stores text label snapshots
 * (not FKs) so history survives status rename/delete.
 */
@Entity
@Table(name = "application_status_history")
@Getter
@Setter
public class ApplicationStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "from_label")
    private String fromLabel;

    @Column(name = "to_label", nullable = false)
    private String toLabel;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    @PrePersist
    void onCreate() {
        changedAt = Instant.now();
    }
}
