package com.jobtracker.backendJobTracker.application.dto;

import java.time.Instant;
import java.util.UUID;

import com.jobtracker.backendJobTracker.application.enums.ApplicationStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicationStatusHistoryResponse {

    private UUID id;

    
    private ApplicationStatus fromStatus;

    private ApplicationStatus toStatus;

    private String note;

    private Instant changedAt;
}
