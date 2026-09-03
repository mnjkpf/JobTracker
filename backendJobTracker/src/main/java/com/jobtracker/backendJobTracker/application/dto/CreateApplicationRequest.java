package com.jobtracker.backendJobTracker.application.dto;

import java.util.UUID;

import com.jobtracker.backendJobTracker.application.enums.ContractType;
import com.jobtracker.backendJobTracker.application.enums.Seniority;
import com.jobtracker.backendJobTracker.application.enums.WorkMode;

import jakarta.validation.constraints.NotBlank;
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
    private String name;

    @NotBlank(message = "Company name is required")
    @Size(max = 255)
    private String companyName;

    @NotBlank(message = "URL is required")
    @Size(max = 2048)
    private String url;

    // Optional — defaults to the user's SAVED-type status in the service layer.
    private UUID statusId;

    @NotNull(message = "Contract type is required")
    private ContractType contractType;

    @NotNull(message = "Seniority is required")
    private Seniority seniority;

    @NotNull(message = "Work mode is required")
    private WorkMode workMode;

    private String description;
    private String notes;

    @Size(max = 255)
    private String location;

    @Positive
    private Integer salaryMin;

    @Positive
    private Integer salaryMax;

    @Size(min = 3, max = 3, message = "Currency must be 3-letter code (PLN, EUR, USD)")
    private String salaryCurrency;
}
