package com.jobtracker.backendJobTracker.cv.tailored.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.jobtracker.backendJobTracker.cv.enums.CvLanguage;

import lombok.Getter;
import lombok.Setter;
 
@Getter
@Setter
public class TailoredCvResponse {
 
    private UUID id;
    private UUID applicationId;
    private Integer version;
 
    private String fullName;
    private String headline;
    private String email;
    private String phone;
    private String linkedInUrl;
    private String githubUrl;
    private String summary;
    private CvLanguage language;
 
    private Instant createdAt;
 
    // Колекції завантажуються службою окремо (TailoredCv не має @OneToMany)
    private List<TailoredExperienceResponse> experiences;
    private List<TailoredEducationResponse> educations;
    private List<TailoredSkillResponse> skills;
    private List<TailoredProjectResponse> projects;
    private List<TailoredLanguageResponse> languages;
}

