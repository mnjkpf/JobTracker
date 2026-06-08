package com.jobtracker.backendJobTracker.cv.dto.parse;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ParsedEducation {
    private String institution;
    private String degree;
    private String fieldOfStudy;
    private String description;
    private String startDate;
    private String endDate;
}
