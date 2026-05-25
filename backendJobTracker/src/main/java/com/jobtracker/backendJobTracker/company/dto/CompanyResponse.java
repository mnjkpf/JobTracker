package com.jobtracker.backendJobTracker.company.dto;

import com.jobtracker.backendJobTracker.company.CampanySize;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyResponse {
    private String id;
    private String name;
    private String website;
    private String industry;
    private String description;
    private CampanySize size;
}
