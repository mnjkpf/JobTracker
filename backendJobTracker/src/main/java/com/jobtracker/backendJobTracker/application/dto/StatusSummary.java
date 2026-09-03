package com.jobtracker.backendJobTracker.application.dto;

import java.util.UUID;

import com.jobtracker.backendJobTracker.application.enums.ApplicationStatus;

import lombok.Getter;
import lombok.Setter;

/** Status of an application, embedded in Application responses. */
@Getter
@Setter
public class StatusSummary {
    private UUID id;
    private String name;
    private String color;
    private ApplicationStatus systemType;
}
