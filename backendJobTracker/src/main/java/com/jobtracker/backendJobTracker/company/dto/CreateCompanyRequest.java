package com.jobtracker.backendJobTracker.company.dto;

import com.jobtracker.backendJobTracker.company.CompanySize;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCompanyRequest {

    private String name;
    private String website;
    private String industry;
    private String description;
    private CompanySize size;

}
