package com.jobtracker.backendJobTracker.interview;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.jobtracker.backendJobTracker.auth.CustomUserDetails;
import com.jobtracker.backendJobTracker.interview.dto.InterviewPrepResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/applications/{appId}/interview-prep")
@RequiredArgsConstructor
public class InterviewPrepController {
 
    private final InterviewPrepService interviewPrepService;
 
    
    @PostMapping("/generate")
    public InterviewPrepResponse generate(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID appId) {
        return interviewPrepService.generate(principal.user().getId(), appId);
    }
 
    
    @GetMapping
    public InterviewPrepResponse get(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID appId) {
        return interviewPrepService.getByApplication(principal.user().getId(), appId);
    }
 
    
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID appId) {
        interviewPrepService.delete(principal.user().getId(), appId);
    }
}

