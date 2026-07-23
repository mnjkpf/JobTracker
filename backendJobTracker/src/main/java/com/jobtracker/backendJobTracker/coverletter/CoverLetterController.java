package com.jobtracker.backendJobTracker.coverletter;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.jobtracker.backendJobTracker.auth.CustomUserDetails;
import com.jobtracker.backendJobTracker.coverletter.dto.CoverLetterResponse;
import com.jobtracker.backendJobTracker.coverletter.dto.CoverLetterSummaryResponse;
import com.jobtracker.backendJobTracker.coverletter.dto.GenerateCoverLetterRequest;
import com.jobtracker.backendJobTracker.coverletter.dto.RefineCoverLetterRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/applications/{appId}/cover-letters")
@RequiredArgsConstructor
public class CoverLetterController {
 
    private final CoverLetterService coverLetterService;
 
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CoverLetterResponse generate(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID appId,
            @Valid @RequestBody GenerateCoverLetterRequest request) {
        return coverLetterService.generate(principal.user().getId(), appId, request);
    }
 
    
    @PostMapping("/{clId}/refine")
    @ResponseStatus(HttpStatus.CREATED)
    public CoverLetterResponse refine(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID appId,
            @PathVariable UUID clId,
            @Valid @RequestBody RefineCoverLetterRequest request) {
        return coverLetterService.refine(principal.user().getId(), appId, clId, request);
    }
 
    
    @GetMapping
    public List<CoverLetterSummaryResponse> list(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID appId) {
        return coverLetterService.list(principal.user().getId(), appId);
    }
 
    
    @GetMapping("/{clId}")
    public CoverLetterResponse getById(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID appId,
            @PathVariable UUID clId) {
        return coverLetterService.getById(principal.user().getId(), clId);
    }
 
    
    @DeleteMapping("/{clId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID appId,
            @PathVariable UUID clId) {
        coverLetterService.delete(principal.user().getId(), clId);
    }
}

