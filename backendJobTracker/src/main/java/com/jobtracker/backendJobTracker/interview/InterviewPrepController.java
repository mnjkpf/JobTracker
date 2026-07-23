package com.jobtracker.backendJobTracker.interview;

import java.util.List;
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

import com.jobtracker.backendJobTracker.application.Application;
import com.jobtracker.backendJobTracker.application.ApplicationRepository;
import com.jobtracker.backendJobTracker.auth.CustomUserDetails;
import com.jobtracker.backendJobTracker.exception.ResourceNotFoundException;
import com.jobtracker.backendJobTracker.interview.dto.InterviewPrepResponse;
import com.jobtracker.backendJobTracker.interview.notes.dto.SimilarInterviewNote;
import com.jobtracker.backendJobTracker.interview.rag.InterviewRagService;
import com.jobtracker.backendJobTracker.interview.rag.dto.RelevantNoteResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/applications/{appId}/interview-prep")
@RequiredArgsConstructor
public class InterviewPrepController {
 
    private final InterviewPrepService interviewPrepService;
    private final InterviewRagService ragService;
    private final ApplicationRepository applicationRepository;
    
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

    @GetMapping("/relevant-notes")
    public List<RelevantNoteResponse> getRelevantNotes(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID appId) {
 
        UUID userId = principal.user().getId();
 
        // Tenant-safe fetch — RagService потребує Application entity з усіма
        // полями для buildQueryText (company, technologies через ApplicationSkill)
        Application app = applicationRepository.findByIdAndUserId(appId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found: " + appId));
 
        // RAG retrieval + mapping internal → public DTO
        return ragService.findRelevantPastNotes(userId, app).stream()
                .map(this::toResponse)
                .toList();
    }
 
    /** Internal SimilarInterviewNote → public RelevantNoteResponse. */
    private RelevantNoteResponse toResponse(SimilarInterviewNote n) {
        RelevantNoteResponse r = new RelevantNoteResponse();
        r.setId(n.getId());
        r.setContent(n.getContent());
        r.setNoteType(n.getNoteType());
        r.setApplicationId(n.getApplicationId());
        r.setSimilarity(n.getSimilarity());
        return r;
    }


}

