package com.jobtracker.backendJobTracker.coverletter.dto;

import java.time.Instant;
import java.util.UUID;

import com.jobtracker.backendJobTracker.coverletter.enums.CoverLetterTone;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CoverLetterSummaryResponse {
 
    private UUID id;
    private Integer version;
    private CoverLetterTone tone;
    private Instant createdAt;
 
    /** Перші ~100 символів content + "..." якщо довше. Дає UI прев'ю на listі. */
    private String contentPreview;
}

