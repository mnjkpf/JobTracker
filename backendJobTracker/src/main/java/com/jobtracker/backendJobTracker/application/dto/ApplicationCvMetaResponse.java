package com.jobtracker.backendJobTracker.application.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicationCvMetaResponse {
    
    private UUID id;
    private String fileName;
    private String contentType;
    private long fileSize;
    private String extractedText;
    private Instant createdAt;
}
