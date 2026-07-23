package com.jobtracker.backendJobTracker.cv.tailored.dto;

import java.time.LocalDate;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TailoredEducationResponse {
    private UUID id;
    private String institution;
    private String degree;
    private String fieldOfStudy;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
}
 

