package com.jobtracker.backendJobTracker.application.dto;

import com.jobtracker.backendJobTracker.application.enums.ContractType;
import com.jobtracker.backendJobTracker.application.enums.Seniority;
import com.jobtracker.backendJobTracker.application.enums.WorkMode;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateApplicationRequest {

        
        @Size(max = 255)
        String name;
 
        String description;
        String notes;
 
        @Size(max = 255)
        String location;
 
        ContractType contractType;
        Seniority seniority;
        WorkMode workMode;
 
        @Positive
        Integer salaryMin;
 
        @Positive
        Integer salaryMax;
 
        @Size(min = 3, max = 3)
        String salaryCurrency;

}
