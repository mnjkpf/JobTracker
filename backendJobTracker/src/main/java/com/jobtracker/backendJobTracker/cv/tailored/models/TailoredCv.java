package com.jobtracker.backendJobTracker.cv.tailored.models;

import java.time.Instant;
import java.util.UUID;

import com.jobtracker.backendJobTracker.application.Application;
import com.jobtracker.backendJobTracker.cv.enums.CvLanguage;

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
@Getter
@Setter
@Table(
    name = "tailored_cvs",   // ВИПРАВЛЕНО: plural як скрізь
    uniqueConstraints = @UniqueConstraint(
        name = "uk_tailored_cvs_app_version",
        columnNames = {"application_id", "version"}
    )
)
public class TailoredCv {
 
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    // ДОДАНО: tailored CV належить конкретній заявці. Без цього вся фіча не має сенсу.
    // Tenant safety через application.user.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;
 
    // ДОДАНО: версіонування. Кожен generate = нова version.
    @Column(nullable = false)
    private Integer version;
 
    // Snapshot контактних даних на момент генерації (копіюється з master CV).
    @Column(name = "full_name")
    private String fullName;
 
    private String headline;
    private String email;
    private String phone;
 
    @Column(name = "linkedin_url")
    private String linkedInUrl;
 
    @Column(name = "github_url")
    private String githubUrl;
 
    @Column(columnDefinition = "TEXT")
    private String summary;
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CvLanguage language;
 
    // ВИДАЛЕНО: sourceFileName, sourceFileHash, extractedAt — це для upload PDF (W4 master flow).
    // Tailored генерується з master CV, ніяких файлів не парситься.
 
    /** Версія промпта для tracking — як у MasterCv. */
    @Column(name = "prompt_version")
    private String promptVersion;
 
    @Column(nullable = false, updatable = false, name = "created_at")
    private Instant createdAt;
 
    // ВИДАЛЕНО: updatedAt + @PreUpdate. Tailored immutable.
 
    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}

