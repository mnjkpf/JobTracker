package com.jobtracker.backendJobTracker.cv.dto.parse;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ParsedExperience {
    private String company;
    private String position;
    private String description;
    private String location;
    private String startDate;
    private String endDate;
}
