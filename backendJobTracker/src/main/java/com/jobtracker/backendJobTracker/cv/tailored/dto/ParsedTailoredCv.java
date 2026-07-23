package com.jobtracker.backendJobTracker.cv.tailored.dto;

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
public class ParsedTailoredCv {
 
    private CvLanguage language;
 
    private String fullName;
    private String headline;
    private String email;
    private String phone;
    private String linkedInUrl;
    private String githubUrl;
    private String summary;
 
    private List<ParsedTailoredExperience> experiences;
    private List<ParsedTailoredEducation> educations;
    private List<ParsedTailoredSkill> skills;
    private List<ParsedTailoredProject> projects;
    private List<ParsedTailoredLanguage> languages;
}
