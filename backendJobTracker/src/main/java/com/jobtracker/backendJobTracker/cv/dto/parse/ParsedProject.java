package com.jobtracker.backendJobTracker.cv.dto.parse;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ParsedProject {
    private String name;
    private String description;
    private String technologies;
    private String url;
    private String startDate;
    private String endDate;
}
