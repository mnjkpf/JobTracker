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
@Table(name = "tailored_experiences")    // ВИПРАВЛЕНО: plural
public class TailoredExperience {
 
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    // ВИПРАВЛЕНО: тип TailoredCv + поле "tailoredCv" + JoinColumn "tailored_cv_id".
    // Раніше було field `masterCv` + JoinColumn `master_cv_id` — плутанина і wrong FK.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tailored_cv_id", nullable = false)
    private TailoredCv tailoredCv;
 
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
 
    @Column(nullable = false, updatable = false, name = "created_at")
    private Instant createdAt;
 
    // ВИДАЛЕНО: updatedAt + @PreUpdate — immutable snapshot.
 
    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}

