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
@Getter
@Setter
public class ApplicationSummaryResponse {
    private UUID id;
    private String companyName;
    private ApplicationStatus applicationStatus;
    private ContractType contractType;
    private Seniority seniority;
    private WorkMode workMode;
    private SourceBoard sourceBoard;
    private Instant appliedAt;
}
