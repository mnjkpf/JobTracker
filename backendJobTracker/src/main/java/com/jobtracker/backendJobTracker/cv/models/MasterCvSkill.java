package com.jobtracker.backendJobTracker.cv.models;

import java.time.Instant;
import java.util.UUID;

import com.jobtracker.backendJobTracker.cv.enums.SkillLevel;

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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "master_cv_skills",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_master_cv_skills",
        columnNames = {"master_cv_id", "skill_id"}
    )
)
@Getter
@Setter
public class MasterCvSkill {
 
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_cv_id", nullable = false)
    private MasterCv masterCv;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;
 
    @Enumerated(EnumType.STRING)
    private SkillLevel level;     // per-user рівень. Nullable — юзер може не знати свого рівня
 
    @Column(nullable = false, updatable = false, name = "created_at")
    private Instant createdAt;
 
    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
 
