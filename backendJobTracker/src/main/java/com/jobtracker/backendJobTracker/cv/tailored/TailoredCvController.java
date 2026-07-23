package com.jobtracker.backendJobTracker.cv.tailored;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import com.jobtracker.backendJobTracker.cv.tailored.ats.AtsScoreResponse;
import com.jobtracker.backendJobTracker.cv.tailored.ats.AtsScoringService;
import com.jobtracker.backendJobTracker.cv.tailored.dto.GenerateTailoredCvRequest;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredCvResponse;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredCvSummaryResponse;
import com.jobtracker.backendJobTracker.cv.tailored.export.DocxExporter;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/applications/{appId}/tailored-cvs")
@RequiredArgsConstructor
public class TailoredCvController {
 
    private final TailoredCvService tailoredCvService;
    private final DocxExporter docxExporter;
    private final AtsScoringService atsScoringService;
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TailoredCvResponse generate(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID appId,
            @Valid @RequestBody GenerateTailoredCvRequest request) {
        return tailoredCvService.generate(principal.user().getId(), appId, request);
    }
 
    
    @GetMapping
    public List<TailoredCvSummaryResponse> list(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID appId) {
        return tailoredCvService.list(principal.user().getId(), appId);
    }
 
    
    @GetMapping("/{tcvId}")
    public TailoredCvResponse getById(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID appId,
            @PathVariable UUID tcvId) {
        return tailoredCvService.getById(principal.user().getId(), tcvId);
    }
 
    
    @DeleteMapping("/{tcvId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID appId,
            @PathVariable UUID tcvId) {
        tailoredCvService.delete(principal.user().getId(), tcvId);
    }

    @GetMapping("/{tcvId}/ats-score")
    public AtsScoreResponse atsScore(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID appId,
            @PathVariable UUID tcvId) {
        return atsScoringService.score(principal.user().getId(), tcvId);
    }

    @GetMapping("/{tcvId}/download")
    public ResponseEntity<byte[]> download(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID appId,
            @PathVariable UUID tcvId) {
        
        TailoredCvResponse cv = tailoredCvService.getById(principal.user().getId(), tcvId);
        byte[] docx = docxExporter.export(cv);
        
        return ResponseEntity.ok()
            .header("Content-Type", 
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            .header("Content-Disposition", 
                "attachment; filename=\"tailored_cv_v" + cv.getVersion() + ".docx\"")
            .body(docx);
    }
}

