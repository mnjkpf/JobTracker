package com.jobtracker.backendJobTracker.application.gap.dto;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TriggerGapAnalysisResponse {
    private UUID applicationId;
    private String status; // e.g. "queued", "in_progress", "completed"
    private String message;
}
