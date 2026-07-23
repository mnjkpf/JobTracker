package com.jobtracker.backendJobTracker.cv.dto;

import java.time.LocalDate;
 
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
 
@Getter
@Setter
public class CreateProjectRequest {
 
    @NotBlank
    @Size(max = 255)
    private String name;
 
    @Size(max = 4000)
    private String description;
 
    @Size(max = 500)
    private String url;
 
    @Size(max = 1000)
    private String technologies;
 
    private LocalDate startDate;
    private LocalDate endDate;
}

