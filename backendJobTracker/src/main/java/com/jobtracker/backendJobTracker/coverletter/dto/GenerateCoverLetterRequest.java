package com.jobtracker.backendJobTracker.coverletter.dto;

import com.jobtracker.backendJobTracker.coverletter.enums.CoverLetterTone;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class GenerateCoverLetterRequest {
 
    @NotNull
    private CoverLetterTone tone;
 
    
    @Size(max = 1000)
    private String additionalInstructions;
}

