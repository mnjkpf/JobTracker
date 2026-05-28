package com.jobtracker.backendJobTracker.application.dto;

import com.jobtracker.backendJobTracker.application.enums.ApplicationStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateStatusRequest {
    @NotNull(message = "Status is required")
    ApplicationStatus status;
 
    @Size(max = 2000, message = "Note must not exceed 2000 characters")
    String note;

}
