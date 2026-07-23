package com.jobtracker.backendJobTracker.cv.dto;

import java.time.LocalDate;
import java.util.UUID;
 
import lombok.Getter;
import lombok.Setter;
 
@Getter
@Setter
public class ProjectResponse {
    private UUID id;
    private String name;
    private String description;
    private String url;
    private String technologies;
    private LocalDate startDate;
    private LocalDate endDate;
}

