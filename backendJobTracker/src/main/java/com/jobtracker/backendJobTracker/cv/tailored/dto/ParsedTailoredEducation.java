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
public class ParsedTailoredEducation {
    private String institution;
    private String degree;
    private String fieldOfStudy;
    private String location;
    private String startDate;
    private String endDate;
    private String description;
}
