package com.jobtracker.backendJobTracker.cv.models;

import java.time.Instant;
import java.util.UUID;

import com.jobtracker.backendJobTracker.cv.enums.LanguageLevel;

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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "languages")
@Getter
@Setter
public class Language {
 
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_cv_id", nullable = false)
    private MasterCv masterCv;
 
    /** "English", "Polish", "Ukrainian". */
    @Column(nullable = false)
    private String name;
 
    /** CEFR scale (A1-C2) + NATIVE. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LanguageLevel level;
 
    @Column(nullable = false, updatable = false, name = "created_at")
    private Instant createdAt;
 
    @Column(nullable = false, name = "updated_at")
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
}

