package com.jobtracker.backendJobTracker.cv.dto;

import java.time.LocalDate;
import java.util.UUID;
 
import lombok.Getter;
import lombok.Setter;
 
@Getter
@Setter
public class ExperienceResponse {
    private UUID id;
    private String position;
    private String company;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;     // null = поточна
    private String description;
}

