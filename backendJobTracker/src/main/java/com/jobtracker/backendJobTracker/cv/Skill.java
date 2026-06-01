package com.jobtracker.backendJobTracker.cv;

import java.time.Instant;
import java.util.UUID;

import com.jobtracker.backendJobTracker.cv.enums.SkillCategory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "skills")
@Getter
@Setter
public class Skill {
 
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    /** Нормалізоване ім'я (lowercase, trimmed). UNIQUE — один скіл на всю систему. */
    @Column(nullable = false, unique = true)
    private String name;
 
    @Enumerated(EnumType.STRING)
    private SkillCategory category;
 
    @Column(nullable = false, updatable = false, name = "created_at")
    private Instant createdAt;
 
    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
 
    
}

