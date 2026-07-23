package com.jobtracker.backendJobTracker.application.dto;

import java.time.Instant;
import java.util.UUID;

import com.jobtracker.backendJobTracker.application.enums.ApplicationStatus;
import com.jobtracker.backendJobTracker.application.enums.ContractType;
import com.jobtracker.backendJobTracker.application.enums.Seniority;
import com.jobtracker.backendJobTracker.application.enums.WorkMode;

import lombok.Getter;
import lombok.Setter;

/**
 * Компактне представлення заявки для list/Kanban view. Містить рівно те, що
 * показує картка: позиція, компанія, статус, теги, зарплата, дати.
 */
@Getter
@Setter
public class ApplicationSummaryResponse {
    private UUID id;
    private String name;
    private String companyName;
    private String location;
    private ApplicationStatus status;
    private Seniority seniority;
    private WorkMode workMode;
    private ContractType contractType;
    private String url;
    private Integer salaryMin;
    private Integer salaryMax;
    private String salaryCurrency;
    private Instant appliedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
