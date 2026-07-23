package com.jobtracker.backendJobTracker.cv.tailored.models;

import java.time.Instant;
import java.util.UUID;

import com.jobtracker.backendJobTracker.cv.enums.LanguageLevel;

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
@Getter
@Setter
@Table(name = "tailored_languages")
public class TailoredLanguage {
 
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    // ВИПРАВЛЕНО: ім'я поля tailoredCv (раніше було masterCv — плутанина).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tailored_cv_id", nullable = false)
    private TailoredCv tailoredCv;
 
    @Column(nullable = false)
    private String name;
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LanguageLevel level;
 
    @Column(nullable = false, updatable = false, name = "created_at")
    private Instant createdAt;
 
    // ВИДАЛЕНО: updatedAt — поле було, але без @PreUpdate, що ніколи б не оновлювало.
    // Immutable snapshot — updatedAt не треба.
 
    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}

