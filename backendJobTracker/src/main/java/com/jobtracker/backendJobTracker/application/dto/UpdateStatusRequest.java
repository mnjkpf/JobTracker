package com.jobtracker.backendJobTracker.application.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateStatusRequest {

    @NotNull(message = "Status id is required")
    private UUID statusId;

    @Size(max = 2000, message = "Note must not exceed 2000 characters")
    private String note;
}
