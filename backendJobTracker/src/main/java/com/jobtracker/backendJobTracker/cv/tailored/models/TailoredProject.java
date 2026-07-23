package com.jobtracker.backendJobTracker.cv.tailored.models;

import java.time.Instant;
import java.time.LocalDate;
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

@Entity
@Getter
@Setter
@Table(name = "tailored_projects")
public class TailoredProject {
 
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    // ВИПРАВЛЕНО: ім'я поля tailoredCv (не masterCv) — JoinColumn був правильний.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tailored_cv_id", nullable = false)
    private TailoredCv tailoredCv;
 
    @Column(nullable = false)
    private String name;
 
    @Column(columnDefinition = "TEXT")
    private String description;
 
    private String url;
 
    @Column(columnDefinition = "TEXT")
    private String technologies;
 
    @Column(name = "start_date")
    private LocalDate startDate;
 
    @Column(name = "end_date")
    private LocalDate endDate;
 
    @Column(nullable = false, updatable = false, name = "created_at")
    private Instant createdAt;
 
    // ВИДАЛЕНО: updatedAt + @PreUpdate — immutable.
 
    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}

