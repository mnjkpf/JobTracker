package com.jobtracker.backendJobTracker.cv.tailored.dto;

import java.time.Instant;
import java.util.UUID;

import com.jobtracker.backendJobTracker.cv.enums.CvLanguage;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TailoredCvSummaryResponse {
    private UUID id;
    private Integer version;
    private String headline;
    private CvLanguage language;
    private Instant createdAt;
}
