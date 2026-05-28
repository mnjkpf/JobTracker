package com.jobtracker.backendJobTracker.application.dto;

import java.time.Instant;
import java.util.List;
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
public class ApplicationFilters {
 
    // Multi-value: ?statuses=APPLIED,SCREENING,INTERVIEW
    private List<ApplicationStatus> statuses;
 
    // Single-value: ?contractType=B2B
    private ContractType contractType;
 
    // Multi-value: ?seniorities=JUNIOR,JUNIOR_PLUS
    private List<Seniority> seniorities;
 
    private WorkMode workMode;
    private SourceBoard sourceBoard;
 
    // Фільтр по UUID компанії — ?companyId=abc-123
    private UUID companyId;
 
    /** Search query — шукає в name і description case-insensitive. */
    private String q;
 
    /** ISO-8601 string у query: ?appliedAfter=2026-01-01T00:00:00Z */
    private Instant appliedAfter;
    private Instant appliedBefore;
 
    /** Мінімальна salary max — для "не показуй мені вакансії з maxSalary < 8000". */
    private Integer minSalary;
}
 

