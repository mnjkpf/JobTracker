package com.jobtracker.backendJobTracker.cv.dto;

import com.jobtracker.backendJobTracker.cv.enums.LanguageLevel;
 
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
 
@Getter
@Setter
public class CreateLanguageRequest {
 
    @NotBlank
    @Size(max = 100)
    private String name;
 
    @NotNull
    private LanguageLevel level;
}

