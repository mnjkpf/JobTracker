package com.jobtracker.backendJobTracker.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.jobtracker.backendJobTracker.application.enums.ContractType;
import com.jobtracker.backendJobTracker.application.enums.Seniority;
import com.jobtracker.backendJobTracker.application.enums.SourceBoard;
import com.jobtracker.backendJobTracker.application.enums.WorkMode;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicationFilters {

    // Multi-value: ?statusIds=uuid1,uuid2
    private List<UUID> statusIds;

    private ContractType contractType;

    // Multi-value: ?seniorities=JUNIOR,JUNIOR_PLUS
    private List<Seniority> seniorities;

    private WorkMode workMode;
    private SourceBoard sourceBoard;

    private UUID companyId;

    /** Search query — name + description, case-insensitive. */
    private String q;

    private Instant appliedAfter;
    private Instant appliedBefore;

    private Integer minSalary;

    private boolean archived;
}
