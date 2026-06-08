package com.jobtracker.backendJobTracker.application;

import java.time.Instant;
import java.util.UUID;

import com.jobtracker.backendJobTracker.application.enums.ApplicationStatus;
import com.jobtracker.backendJobTracker.application.enums.ContractType;
import com.jobtracker.backendJobTracker.application.enums.Seniority;
import com.jobtracker.backendJobTracker.application.enums.SourceBoard;
import com.jobtracker.backendJobTracker.application.enums.WorkMode;
import com.jobtracker.backendJobTracker.company.Company;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "applications",
    // ДОДАНО: composite unique. Юзер не може мати дві заявки на ту саму URL,
    // але різні юзери — можуть. Глобальний unique на url зломав би це.
    uniqueConstraints = @UniqueConstraint(
        name = "uk_applications_user_url",
        columnNames = {"user_id", "url"}
    )
)
@Getter
@Setter
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "contract_type")
    private ContractType contractType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Seniority seniority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "work_mode")
    private WorkMode workMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "source_board")
    private SourceBoard sourceBoard;

    // ВИПРАВЛЕНО: location було nullable=false, але багато remote-вакансій
    // не мають конкретного location. Тепер nullable — це OK.
    private String location;

    @Column(name = "salary_min")
    private Integer salaryMin;

    @Column(name = "salary_max")
    private Integer salaryMax;

    @Column(name = "salary_currency", length = 3)
    private String salaryCurrency;

    /** Коли юзер реально подався (status перейшов в APPLIED). Nullable якщо статус SAVED. */
    @Column(name = "applied_at")
    private Instant appliedAt;

    // ДОДАНО: soft delete. DELETE endpoint ставить archived=true, не видаляє рядок.
    @Column(nullable = false)
    private boolean archived = false;

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