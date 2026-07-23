package com.jobtracker.backendJobTracker.cv.dto;

import java.time.LocalDate;
 
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
 
@Getter
@Setter
public class UpdateExperienceRequest {
 
    @Size(max = 255)
    private String position;
 
    @Size(max = 255)
    private String company;
 
    @Size(max = 255)
    private String location;
 
    private LocalDate startDate;
    private LocalDate endDate;
 
    @Size(max = 4000)
    private String description;
}

