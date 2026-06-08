package com.jobtracker.backendJobTracker.company;

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
import com.jobtracker.backendJobTracker.company.dto.CompanyResponse;
import com.jobtracker.backendJobTracker.company.dto.CreateCompanyRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CreateCompanyRequest request) {
        return companyService.create(principal.user().getId(), request);
    }

    /**
     * List з опціональними фільтрами. Жоден / обидва filter params:
     *   GET /companies
     *   GET /companies?industry=Fintech
     *   GET /companies?size=STARTUP
     */
    @GetMapping
    public List<CompanyResponse> list(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) CompanySize size) {
        UUID userId = principal.user().getId();
        if (industry != null) {
            return companyService.getByIndustry(userId, industry);
        }
        if (size != null) {
            return companyService.getBySize(userId, size);
        }
        return companyService.getAll(userId);
    }

    @GetMapping("/{id}")
    public CompanyResponse getById(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID id) {
        return companyService.getById(principal.user().getId(), id);
    }

    @PatchMapping("/{id}")
    public CompanyResponse update(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID id,
            @Valid @RequestBody CreateCompanyRequest request) {
        return companyService.update(principal.user().getId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID id) {
        companyService.delete(principal.user().getId(), id);
    }
}