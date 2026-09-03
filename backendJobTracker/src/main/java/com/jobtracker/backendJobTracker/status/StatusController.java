package com.jobtracker.backendJobTracker.status;

import java.util.List;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.jobtracker.backendJobTracker.auth.CustomUserDetails;
import com.jobtracker.backendJobTracker.status.dto.CreateStatusRequest;
import com.jobtracker.backendJobTracker.status.dto.ReorderStatusesRequest;
import com.jobtracker.backendJobTracker.status.dto.StatusResponse;
import com.jobtracker.backendJobTracker.status.dto.UpdateStatusCategoryRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/statuses")
@RequiredArgsConstructor
public class StatusController {

    private final StatusService statusService;

    @GetMapping
    public List<StatusResponse> list(@AuthenticationPrincipal CustomUserDetails principal) {
        return statusService.list(principal.user().getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StatusResponse create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CreateStatusRequest request) {
        return statusService.create(principal.user().getId(), request);
    }

    @PatchMapping("/{id}")
    public StatusResponse update(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusCategoryRequest request) {
        return statusService.update(principal.user().getId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID id,
            @RequestParam(required = false) UUID reassignTo) {
        statusService.delete(principal.user().getId(), id, reassignTo);
    }

    @PatchMapping("/reorder")
    public List<StatusResponse> reorder(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ReorderStatusesRequest request) {
        return statusService.reorder(principal.user().getId(), request.getOrderedIds());
    }
}
