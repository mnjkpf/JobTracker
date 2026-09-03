package com.jobtracker.backendJobTracker.status.dto;

import java.util.UUID;

import com.jobtracker.backendJobTracker.application.enums.ApplicationStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusResponse {
    private UUID id;
    private String name;
    private String color;
    private int position;
    private ApplicationStatus systemType;
    private boolean terminal;
}
