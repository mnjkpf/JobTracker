package com.jobtracker.backendJobTracker.cv.dto;

import java.time.Instant;
import java.util.UUID;

public record ApplicationCvMeta(
        UUID id,
        String fileName,
        String contentType,
        long fileSize,
        String extractedText,
        Instant createdAt
) {
}
