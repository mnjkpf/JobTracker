package com.jobtracker.backendJobTracker.company.dto;


import java.util.UUID;

import com.jobtracker.backendJobTracker.company.CompanySize;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyResponse {
    private UUID id;
    private String companyName;
    private String website;
    private String industry;
    private String description;
    private CompanySize size;
}
