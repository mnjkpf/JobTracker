package com.jobtracker.backendJobTracker.cv.tailored.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
 
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedTailoredExperience {
    private String position;
    private String company;
    private String location;
    private String startDate;     // String — LLM повертає різні формати
    private String endDate;
    private String description;
}

