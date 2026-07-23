package com.jobtracker.backendJobTracker.cv.dto;

import java.time.Instant;
import java.util.UUID;
 
import com.jobtracker.backendJobTracker.cv.enums.CvLanguage;
 
import lombok.Getter;
import lombok.Setter;
 

@Getter
@Setter
public class MasterCvSummaryResponse {
 
    private UUID id;
    private String fullName;
    private String headline;
    private CvLanguage language;
    private Instant updatedAt;
}
