package com.jobtracker.backendJobTracker.coverletter.dto;

import java.time.Instant;
import java.util.UUID;

import com.jobtracker.backendJobTracker.coverletter.enums.CoverLetterTone;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CoverLetterResponse {
    private UUID id;


    private String content;

    private int version;

    
    private CoverLetterTone tone;

    private Instant createdAt;
}
