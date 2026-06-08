package com.jobtracker.backendJobTracker.cv.dto;

import com.jobtracker.backendJobTracker.cv.enums.LanguageLevel;
 
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
 
@Getter
@Setter
public class UpdateLanguageRequest {
 
    @Size(max = 100)
    private String name;
 
    private LanguageLevel level;
}
