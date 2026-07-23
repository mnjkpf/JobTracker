package com.jobtracker.backendJobTracker.interview.notes;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.jobtracker.backendJobTracker.auth.CustomUserDetails;
import com.jobtracker.backendJobTracker.interview.notes.dto.CreateNoteRequest;
import com.jobtracker.backendJobTracker.interview.notes.dto.InterviewNoteResponse;
import com.jobtracker.backendJobTracker.interview.notes.dto.PostInterviewReflectionRequest;
import com.jobtracker.backendJobTracker.interview.notes.dto.UpdateNoteRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/v1/applications/{appId}/interview-prep/notes")
@RequiredArgsConstructor
public class InterviewNoteController {

    private final InterviewNoteService noteService;

    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InterviewNoteResponse create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID appId,
            @Valid @RequestBody CreateNoteRequest request) {
        return noteService.createNote(principal.user().getId(), appId, request);
    }

    
    @GetMapping
    public List<InterviewNoteResponse> list(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID appId) {
        return noteService.list(principal.user().getId(), appId);
    }

    
    @PutMapping("/{noteId}")
    public InterviewNoteResponse update(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID appId,
            @PathVariable UUID noteId,
            @Valid @RequestBody UpdateNoteRequest request) {
        return noteService.updateNote(principal.user().getId(), noteId, request);
    }

    
    @DeleteMapping("/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID appId,
            @PathVariable UUID noteId) {
        noteService.delete(principal.user().getId(), noteId);
    }

    
    @PostMapping("/reflection")
    @ResponseStatus(HttpStatus.CREATED)
    public InterviewNoteResponse addReflection(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID appId,
            @Valid @RequestBody PostInterviewReflectionRequest request) {
        return noteService.addPostInterviewReflection(principal.user().getId(), appId, request);
    }
}