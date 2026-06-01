package com.jobtracker.backendJobTracker.cv;

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
@Table(name = "experiences")
@Getter
@Setter
public class Experience {
 
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_cv_id", nullable = false)
    private MasterCv masterCv;
 
    // ВИПРАВЛЕНО: position замість title — стандартна термінологія для роботи.
    @Column(nullable = false)
    private String position;
 
    @Column(nullable = false)
    private String company;
 
    private String location;
 
    
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
 
    @Column(name = "end_date")
    private LocalDate endDate;
 
    @Column(columnDefinition = "TEXT")
    private String description;
 
    // ВИПРАВЛЕНО: додано @Column constraints для createdAt (раніше були тільки на updatedAt).
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

