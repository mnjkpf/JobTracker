package com.jobtracker.backendJobTracker.application;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.jobtracker.backendJobTracker.application.dto.ApplicationFilters;
import com.jobtracker.backendJobTracker.application.dto.ApplicationResponse;
import com.jobtracker.backendJobTracker.application.dto.ApplicationStatusHistoryResponse;
import com.jobtracker.backendJobTracker.application.dto.ApplicationSummaryResponse;
import com.jobtracker.backendJobTracker.application.dto.CreateApplicationRequest;
import com.jobtracker.backendJobTracker.application.dto.UpdateApplicationRequest;
import com.jobtracker.backendJobTracker.application.dto.UpdateStatusRequest;
import com.jobtracker.backendJobTracker.application.gap.GapAnalysisService;
import com.jobtracker.backendJobTracker.application.gap.dto.GapAnalysisResponse;
import com.jobtracker.backendJobTracker.application.parsing.dto.ParseUrlPreviewResponse;
import com.jobtracker.backendJobTracker.application.parsing.dto.ParseUrlRequest;
import com.jobtracker.backendJobTracker.auth.CustomUserDetails;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {
 
    private final ApplicationService applicationService;
    private final GapAnalysisService gapAnalysisService;
 
    // ─── Manual CRUD ────────────────────────────────────────────────
 
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CreateApplicationRequest request) {
        return applicationService.create(principal.user().getId(), request);
    }
 
    @GetMapping
    public Page<ApplicationSummaryResponse> list(
            @AuthenticationPrincipal CustomUserDetails principal,
            @ModelAttribute ApplicationFilters filters,
            Pageable pageable) {
        return applicationService.list(principal.user().getId(), filters, pageable);
    }
 
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
 
    @GetMapping("/{id}/status-history")
    public List<ApplicationStatusHistoryResponse> getStatusHistory(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID id) {
        return applicationService.getStatusHistory(principal.user().getId(), id);
    }
 
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID id) {
        applicationService.archive(principal.user().getId(), id);
    }
 
    // ─── URL-based parsing (3B) ─────────────────────────────────────
 
    /**
     * Preview: парсить URL, повертає що витягнулось, НЕ зберігає в БД.
     * Юзер виправляє у формі і потім робить POST /applications для збереження.
     * <p>
     * Не повертає 201 Created — нічого не створено. Просто 200 OK з даними.
     */
    @PostMapping("/parse-url")
    public ParseUrlPreviewResponse parseUrl(@Valid @RequestBody ParseUrlRequest request) {
        return applicationService.previewFromUrl(request.getUrl());
    }
 
    /**
     * Парсить URL і одразу створює заявку. Швидкий шлях коли юзер довіряє парсеру.
     * Запис створюється зі статусом SAVED — юзер ще не подавався.
     */
    @PostMapping("/from-url")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse createFromUrl(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ParseUrlRequest request) {
        return applicationService.createFromUrl(principal.user().getId(), request.getUrl());
    }

    @PostMapping("/{id}/gap-analysis")
    public GapAnalysisResponse runGapAnalysis(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID id) {
        return gapAnalysisService.runAnalysis(principal.user().getId(), id);
    }
    
    /**
     * Refresh результату без повторної LLM-екстракції. Швидко — лише pgvector
     * similarity проти збережених ApplicationSkills.
     * <p>
     * 422 (BusinessRuleException) якщо нема CV або ще не запускав runAnalysis.
     */
    @GetMapping("/{id}/gap-analysis")
    public GapAnalysisResponse getGapAnalysis(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID id) {
        return gapAnalysisService.getAnalysis(principal.user().getId(), id);
    }

}
