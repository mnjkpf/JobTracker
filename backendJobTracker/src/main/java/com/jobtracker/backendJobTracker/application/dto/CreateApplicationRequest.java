package com.jobtracker.backendJobTracker.application.dto;

import org.hibernate.validator.constraints.NotBlank;

import com.jobtracker.backendJobTracker.application.enums.ApplicationStatus;
import com.jobtracker.backendJobTracker.application.enums.ContractType;
import com.jobtracker.backendJobTracker.application.enums.Seniority;
import com.jobtracker.backendJobTracker.application.enums.WorkMode;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateApplicationRequest {
    
 
        @NotBlank(message = "Position name is required")
        @Size(max = 255)
        String name;
 
        @NotBlank(message = "Company name is required")
        @Size(max = 255)
        String companyName;
 
        @NotBlank(message = "URL is required")
        @Size(max = 2048)
        String url;
 
        // Status опціональний — defaults до SAVED у service-шарі.
        // Юзер може одразу вказати APPLIED якщо подався не через цей tool.
        ApplicationStatus status;
 
        @NotNull(message = "Contract type is required")
        ContractType contractType;
 
        @NotNull(message = "Seniority is required")
        Seniority seniority;
 
        @NotNull(message = "Work mode is required")
        WorkMode workMode;
 
        // Опціональні описові поля
        String description;
        String notes;
 
        @Size(max = 255)
        String location;
 
        // Опціональний salary. Може бути просто min, просто max, або обидва.
        // Якщо обидва — service перевірить min <= max (через CHECK constraint у БД також).
        @Positive
        Integer salaryMin;
 
        @Positive
        Integer salaryMax;
 
        @Size(min = 3, max = 3, message = "Currency must be 3-letter code (PLN, EUR, USD)")
        String salaryCurrency;

}
