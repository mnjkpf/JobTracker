package com.jobtracker.backendJobTracker.interview;

import java.time.Instant;
import java.util.UUID;

import com.jobtracker.backendJobTracker.application.Application;
import com.jobtracker.backendJobTracker.interview.enums.InterviewPrepStatus;

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
@Getter
@Setter
@Table(name = "interview_preps")
public class InterviewPrep {
 
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    // unique=true тут — для документації наміру (1:1 з Application). Реальний
    // UNIQUE constraint у SQL міграції — це source of truth.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private Application application;
 
    // ДОДАНО: nullable=false для status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewPrepStatus status;
 
    
    @Column(name = "prompt_version")
    private String promptVersion;
 
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
 
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
 
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }
 
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

