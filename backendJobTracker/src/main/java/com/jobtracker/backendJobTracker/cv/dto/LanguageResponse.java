package com.jobtracker.backendJobTracker.cv.dto;

import java.util.UUID;
 
import com.jobtracker.backendJobTracker.cv.enums.LanguageLevel;
 
import lombok.Getter;
import lombok.Setter;
 
@Getter
@Setter
public class LanguageResponse {
    private UUID id;
    private String name;
    private LanguageLevel level;
}
