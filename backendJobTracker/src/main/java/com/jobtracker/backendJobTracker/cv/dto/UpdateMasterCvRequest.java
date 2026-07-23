package com.jobtracker.backendJobTracker.cv.dto;

import com.jobtracker.backendJobTracker.cv.enums.CvLanguage;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMasterCvRequest {
 
    @Size(max = 255)
    private String fullName;
 
    @Size(max = 255)
    private String headline;
 
    @Email
    @Size(max = 255)
    private String email;
 
    @Size(max = 50)
    private String phone;
 
    @Size(max = 500)
    private String linkedInUrl;
 
    @Size(max = 500)
    private String githubUrl;
 
    @Size(max = 4000)
    private String summary;
 
    private CvLanguage language;
}
