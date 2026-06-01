package com.jobtracker.backendJobTracker.application.parsing;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.stereotype.Component;

import com.jobtracker.backendJobTracker.application.enums.SourceBoard;

/**
 * Визначає job board за доменом URL. Невідомий домен → OTHER (піде на LLM fallback).
 */
@Component
public class JobBoardDetector {
 
    public SourceBoard detect(String url) {
        String host = extractHost(url);
        if (host == null) {
            return SourceBoard.OTHER;
        }
 
        host = host.toLowerCase().replaceFirst("^www\\.", "");
 
        if (host.contains("nofluffjobs.com")) {
            return SourceBoard.NOFLUFFJOBS;
        }
        if (host.contains("justjoin.it")) {
            return SourceBoard.JUSTJOINIT;
        }
        if (host.contains("pracuj.pl")) {
            return SourceBoard.PRACUJ;
        }
        if (host.contains("linkedin.com")) {
            return SourceBoard.LINKEDIN;
        }
 
        return SourceBoard.OTHER;
    }
 
    private String extractHost(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(url.trim());
            return uri.getHost();
        } catch (URISyntaxException e) {
            return null;
        }
    }
}

