package com.jobtracker.backendJobTracker.cv.dto;

import java.time.LocalDate;
 
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
 
@Getter
@Setter
public class CreateExperienceRequest {
 
    @NotBlank
    @Size(max = 255)
    private String position;
 
    @NotBlank
    @Size(max = 255)
    private String company;
 
    @Size(max = 255)
    private String location;
 
    @NotNull
    private LocalDate startDate;
 
    private LocalDate endDate;     // null = поточна
 
    @Size(max = 4000)
    private String description;
}

