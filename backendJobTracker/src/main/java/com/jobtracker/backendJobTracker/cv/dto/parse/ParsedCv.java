package com.jobtracker.backendJobTracker.cv.dto.parse;

import java.util.List;

import com.jobtracker.backendJobTracker.cv.enums.CvLanguage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Getter 
@Setter 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class ParsedCv {
    private CvLanguage language;       
    // Hero блок
    private String fullName;
    private String headline;
    private String email;
    private String phone;
    private String linkedInUrl;
    private String githubUrl;
    private String summary;

    // Колекції — кожна як List<Parsed*> sub-DTO
    private List<ParsedExperience> experiences;
    private List<ParsedEducation> educations;
    private List<ParsedSkill> skills;
    private List<ParsedProject> projects;
    private List<ParsedLanguage> languages;
}
