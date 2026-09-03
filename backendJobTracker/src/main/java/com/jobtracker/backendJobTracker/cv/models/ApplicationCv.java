package com.jobtracker.backendJobTracker.cv.models;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "application_cvs")
@Getter
@Setter
public class ApplicationCv {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // 1 CV per application. Plain FK column (no association needed here).
    @Column(name = "application_id", nullable = false, unique = true)
    private UUID applicationId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    // Never loaded on metadata reads (projection excludes it); only on /download.
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "file_bytes", nullable = false)
    private byte[] fileBytes;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

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
