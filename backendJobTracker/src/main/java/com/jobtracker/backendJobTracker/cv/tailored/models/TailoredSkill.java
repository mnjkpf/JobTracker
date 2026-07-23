package com.jobtracker.backendJobTracker.cv.tailored.models;

import java.time.Instant;
import java.util.UUID;

import com.jobtracker.backendJobTracker.cv.enums.SkillCategory;
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
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity
@Table(name = "tailored_skills")
public class TailoredSkill {
 
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    // ВИПРАВЛЕНО: посилання на TailoredCv (не MasterCv!).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tailored_cv_id", nullable = false)
    private TailoredCv tailoredCv;
 
    // ВИДАЛЕНО: FK на Skill catalog. Tailored — це snapshot, не junction.
    // ДОДАНО: name як простий String.
    @Column(nullable = false)
    private String name;
 
    // ДОДАНО: category як копія з MasterCvSkill.skill.category на момент генерації.
    @Enumerated(EnumType.STRING)
    private SkillCategory category;
 
    @Enumerated(EnumType.STRING)
    private SkillLevel level;
 
    @Column(nullable = false, updatable = false, name = "created_at")
    private Instant createdAt;
 
    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
