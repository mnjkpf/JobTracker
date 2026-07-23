package com.jobtracker.backendJobTracker.application.gap;

import java.time.Instant;
import java.util.UUID;

import com.jobtracker.backendJobTracker.application.Application;
import com.jobtracker.backendJobTracker.cv.models.Skill;

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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;



@Entity
@Table(
    name = "application_skills",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_application_skills",
        columnNames = {"application_id", "skill_id"}
    )
)
@Getter
@Setter
public class ApplicationSkill {
 
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;
 
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;
 
    
    @Column(nullable = false)
    private boolean required;
 
    @Column(nullable = false, updatable = false, name = "created_at")
    private Instant createdAt;
 
    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}

