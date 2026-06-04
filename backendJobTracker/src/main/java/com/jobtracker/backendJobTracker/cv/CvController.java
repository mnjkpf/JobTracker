package com.jobtracker.backendJobTracker.cv;

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

import com.jobtracker.backendJobTracker.auth.CustomUserDetails;
import com.jobtracker.backendJobTracker.cv.dto.AddSkillRequest;
import com.jobtracker.backendJobTracker.cv.dto.CreateEducationRequest;
import com.jobtracker.backendJobTracker.cv.dto.CreateExperienceRequest;
import com.jobtracker.backendJobTracker.cv.dto.CreateLanguageRequest;
import com.jobtracker.backendJobTracker.cv.dto.CreateProjectRequest;
import com.jobtracker.backendJobTracker.cv.dto.EducationResponse;
import com.jobtracker.backendJobTracker.cv.dto.ExperienceResponse;
import com.jobtracker.backendJobTracker.cv.dto.LanguageResponse;
import com.jobtracker.backendJobTracker.cv.dto.MasterCvResponse;
import com.jobtracker.backendJobTracker.cv.dto.ProjectResponse;
import com.jobtracker.backendJobTracker.cv.dto.SkillResponse;
import com.jobtracker.backendJobTracker.cv.dto.UpdateEducationRequest;
import com.jobtracker.backendJobTracker.cv.dto.UpdateExperienceRequest;
import com.jobtracker.backendJobTracker.cv.dto.UpdateLanguageRequest;
import com.jobtracker.backendJobTracker.cv.dto.UpdateMasterCvRequest;
import com.jobtracker.backendJobTracker.cv.dto.UpdateProjectRequest;
import com.jobtracker.backendJobTracker.cv.dto.UpdateSkillRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/cv")
@RequiredArgsConstructor
public class CvController {
 
    private final CvService cvService;
 
    @GetMapping("/me")
    public MasterCvResponse getMy(@AuthenticationPrincipal CustomUserDetails principal) {
        return cvService.getMy(principal.user().getId());
    }
 
    @PatchMapping("/me")
    public MasterCvResponse updateMetadata(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UpdateMasterCvRequest request) {
        return cvService.updateMetadata(principal.user().getId(), request);
    }

 
    @PostMapping("/me/experiences")
    @ResponseStatus(HttpStatus.CREATED)
    public ExperienceResponse addExperience(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CreateExperienceRequest request) {
        return cvService.addExperience(principal.user().getId(), request);
    }
 
    @PatchMapping("/me/experiences/{id}")
    public ExperienceResponse updateExperience(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExperienceRequest request) {
        return cvService.updateExperience(principal.user().getId(), id, request);
    }
 
    @DeleteMapping("/me/experiences/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExperience(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID id) {
        cvService.deleteExperience(principal.user().getId(), id);
    }
 
    
 
    @PostMapping("/me/educations")
    @ResponseStatus(HttpStatus.CREATED)
    public EducationResponse addEducation(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CreateEducationRequest request) {
        return cvService.addEducation(principal.user().getId(), request);
    }
 
    @PatchMapping("/me/educations/{id}")
    public EducationResponse updateEducation(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEducationRequest request) {
        return cvService.updateEducation(principal.user().getId(), id, request);
    }
 
    @DeleteMapping("/me/educations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEducation(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID id) {
        cvService.deleteEducation(principal.user().getId(), id);
    }
 
    
 
    @PostMapping("/me/skills")
    @ResponseStatus(HttpStatus.CREATED)
    public SkillResponse addSkill(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody AddSkillRequest request) {
        return cvService.addSkill(principal.user().getId(), request);
    }
 
    @PatchMapping("/me/skills/{id}")
    public SkillResponse updateSkillLevel(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSkillRequest request) {
        return cvService.updateSkillLevel(principal.user().getId(), id, request);
    }
 
    @DeleteMapping("/me/skills/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSkill(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID id) {
        cvService.removeSkill(principal.user().getId(), id);
    }
 
    
 
    @PostMapping("/me/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse addProject(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CreateProjectRequest request) {
        return cvService.addProject(principal.user().getId(), request);
    }
 
    @PatchMapping("/me/projects/{id}")
    public ProjectResponse updateProject(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProjectRequest request) {
        return cvService.updateProject(principal.user().getId(), id, request);
    }
 
    @DeleteMapping("/me/projects/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID id) {
        cvService.deleteProject(principal.user().getId(), id);
    }
 
    
 
    @PostMapping("/me/languages")
    @ResponseStatus(HttpStatus.CREATED)
    public LanguageResponse addLanguage(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CreateLanguageRequest request) {
        return cvService.addLanguage(principal.user().getId(), request);
    }
 
    @PatchMapping("/me/languages/{id}")
    public LanguageResponse updateLanguage(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLanguageRequest request) {
        return cvService.updateLanguage(principal.user().getId(), id, request);
    }
 
    @DeleteMapping("/me/languages/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLanguage(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID id) {
        cvService.deleteLanguage(principal.user().getId(), id);
    }

}