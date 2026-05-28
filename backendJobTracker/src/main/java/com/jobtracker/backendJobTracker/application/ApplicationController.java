package com.jobtracker.backendJobTracker.application;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.jobtracker.backendJobTracker.application.dto.ApplicationResponse;
import com.jobtracker.backendJobTracker.application.dto.CreateApplicationRequest;
import com.jobtracker.backendJobTracker.application.dto.UpdateApplicationRequest;
import com.jobtracker.backendJobTracker.application.dto.UpdateStatusRequest;
import com.jobtracker.backendJobTracker.auth.CustomUserDetails;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CreateApplicationRequest request) {
        return applicationService.create(principal.user().getId(), request);
    }

    // @GetMapping
    // public Page<ApplicationSummaryResponse> list(
    //         @AuthenticationPrincipal CustomUserDetails principal,
    //         @ModelAttribute ApplicationFilters filters,
    //         Pageable pageable) {
    //     return applicationService.list(principal.user().getId(), filters, pageable);
    // }

    @GetMapping("/{id}")
    public ApplicationResponse getById(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID id) {
        return applicationService.getById(principal.user().getId(), id);
    }

    @PatchMapping("/{id}")
    public ApplicationResponse updateDetails(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateApplicationRequest request) {
        return applicationService.updateDetails(principal.user().getId(), id, request);
    }

    @PatchMapping("/{id}/status")
    public ApplicationResponse updateStatus(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request) {
        return applicationService.updateStatus(principal.user().getId(), id, request);
    }

    // @GetMapping("/{id}/status-history")
    // public List<ApplicationStatusHistoryResponse> getStatusHistory(
    //         @AuthenticationPrincipal CustomUserDetails principal,
    //         @PathVariable UUID id) {
    //     return applicationService.getStatusHistory(principal.user().getId(), id);
    // }

    /** Soft delete (archive). Стандартний "delete" для UI. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID id) {
        applicationService.archive(principal.user().getId(), id);
    }
}