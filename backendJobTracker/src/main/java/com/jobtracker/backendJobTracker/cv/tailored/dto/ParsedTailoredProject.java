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
public class ParsedTailoredProject {
    private String name;
    private String description;
    private String url;
    private String technologies;
    private String startDate;
    private String endDate;
}
 

