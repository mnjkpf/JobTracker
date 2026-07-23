package com.jobtracker.backendJobTracker.cv.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
 
import com.jobtracker.backendJobTracker.cv.enums.CvLanguage;
 
import lombok.Getter;
import lombok.Setter;
 
@Getter
@Setter
public class MasterCvResponse {
 
    private UUID id;
    private String fullName;
    private String headline;
    private String email;
    private String phone;
    private String linkedInUrl;
    private String githubUrl;
    private String summary;
    private CvLanguage language;
    private Instant updatedAt;
 
    // Вкладені колекції — повний CV в одній відповіді
    private List<ExperienceResponse> experiences;
    private List<EducationResponse> educations;
    private List<SkillResponse> skills;
    private List<ProjectResponse> projects;
    private List<LanguageResponse> languages;
}
