package com.jobtracker.backendJobTracker.cv.models;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "educations")
@Getter
@Setter
public class Education {
 
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_cv_id", nullable = false)
    private MasterCv masterCv;
 
    @Column(nullable = false)
    private String institution;          // "WSB University", "Politechnika Warszawska"
 
    private String degree;               // "Bachelor", "Master", "Engineer"
 
    @Column(name = "field_of_study")
    private String fieldOfStudy;         // "Computer Science", "Informatyka"
 
    private String location;
 
    // LocalDate, не util.Date
    @Column(name = "start_date")
    private LocalDate startDate;
 
    @Column(name = "end_date")
    private LocalDate endDate;
 
    @Column(columnDefinition = "TEXT")
    private String description;
 
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

