package com.jobtracker.backendJobTracker.application.dto;

import java.time.Instant;
import java.util.UUID;

import com.jobtracker.backendJobTracker.application.enums.ApplicationStatus;
import com.jobtracker.backendJobTracker.application.enums.ContractType;
import com.jobtracker.backendJobTracker.application.enums.Seniority;
import com.jobtracker.backendJobTracker.application.enums.SourceBoard;
import com.jobtracker.backendJobTracker.application.enums.WorkMode;

import lombok.Getter;
import lombok.Setter;

/**
 * Full application detail (GET /applications/{id}). Same field naming as
 * ApplicationSummaryResponse plus description/notes-free detail view fields.
 */
@Getter
@Setter
public class ApplicationResponse {

    private UUID id;
    private String name;
    private String description;
    private String url;
    private String companyName;
    private String location;
    private ApplicationStatus status;
    private Seniority seniority;
    private WorkMode workMode;
    private ContractType contractType;
    private SourceBoard sourceBoard;
    private Integer salaryMin;
    private Integer salaryMax;
    private String salaryCurrency;
    private Instant appliedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
