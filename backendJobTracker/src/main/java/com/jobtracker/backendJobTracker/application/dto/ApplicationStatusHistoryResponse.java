package com.jobtracker.backendJobTracker.application.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicationStatusHistoryResponse {
    private UUID id;
    private String fromLabel;
    private String toLabel;
    private String note;
    private Instant changedAt;
}
