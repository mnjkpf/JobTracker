package com.jobtracker.backendJobTracker.cv;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jobtracker.backendJobTracker.application.dto.ApplicationCvMetaResponse;
import com.jobtracker.backendJobTracker.auth.CustomUserDetails;
import com.jobtracker.backendJobTracker.cv.models.ApplicationCv;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/applications/{applicationId}/cv")
public class ApplicationCvController {

    private final ApplicationCvService applicationCvService;

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationCvMetaResponse uploadCv(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID applicationId,
            @RequestParam("file") MultipartFile file) {
        return applicationCvService.upload(principal.user().getId(), applicationId, file);
    }

    @GetMapping("/meta")
    public ApplicationCvMetaResponse getCvMeta(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID applicationId) {
        return applicationCvService.getMeta(principal.user().getId(), applicationId);
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> download(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID applicationId) {
        ApplicationCv cv = applicationCvService.getDownload(principal.user().getId(), applicationId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(cv.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + cv.getFileName() + "\"")
                .body(cv.getFileBytes());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCv(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID applicationId) {
        applicationCvService.delete(principal.user().getId(), applicationId);
    }
}
