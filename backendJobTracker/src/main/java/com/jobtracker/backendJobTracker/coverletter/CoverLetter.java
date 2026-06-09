package com.jobtracker.backendJobTracker.coverletter;

import java.time.Instant;
import java.util.UUID;

import com.jobtracker.backendJobTracker.application.Application;
import com.jobtracker.backendJobTracker.coverletter.enums.CoverLetterTone;

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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cover_letters")
@Getter
@Setter
public class CoverLetter {
 
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;
 
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
 
    
    @Column(nullable = false)
    private Integer version;
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CoverLetterTone tone;
 
    @Column(nullable = false, updatable = false, name = "created_at")
    private Instant createdAt;
 
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
 
    
}

