package com.jobtracker.backendJobTracker.cv.models;

import java.time.Instant;
import java.util.UUID;

import com.jobtracker.backendJobTracker.cv.enums.CvLanguage;
import com.jobtracker.backendJobTracker.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "master_cvs")
@Getter
@Setter
public class MasterCv {
 
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    // ВИПРАВЛЕНО: @OneToOne з UNIQUE — один CV на юзера.
    // Раніше @ManyToOne дозволяв багато master CV для одного юзера (порушення W4 plan).
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
 
    // ─── Контактна / hero блок ───────────────────────────────────────
    // Витягається з parsed CV. Може відрізнятись від User.email (login email).
 
    @Column(name = "full_name")
    private String fullName;
 
    /** Короткий tagline під ім'ям: "Junior Java Developer", "Software Engineer". */
    private String headline;
 
    private String email;
    private String phone;
 
    @Column(name = "linkedin_url")
    private String linkedInUrl;
 
    @Column(name = "github_url")
    private String githubUrl;
 
    /** 2-4 речення "про себе". TEXT бо може бути довгим. */
    @Column(columnDefinition = "TEXT")
    private String summary;
 
    
 
    /** Мова оригінального CV. Впливає на LLM extraction і W6 generation. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CvLanguage language;
 
    /** Назва файлу з якого парсили (для tracking, debug). */
    @Column(name = "source_file_name")
    private String sourceFileName;
 
    /** SHA-256 hash файлу — кешування LLM extraction за тим самим файлом. */
    @Column(name = "source_file_hash")
    private String sourceFileHash;
 
    /** Версія промпту яким парсили. Якщо змінимо промпт у майбутньому — будемо знати які CV reextract. */
    @Column(name = "extraction_prompt_version")
    private String extractionPromptVersion;
 
    /** Коли востаннє реекстрактили. null = створено вручну, без upload. */
    @Column(name = "extracted_at")
    private Instant extractedAt;
 
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

