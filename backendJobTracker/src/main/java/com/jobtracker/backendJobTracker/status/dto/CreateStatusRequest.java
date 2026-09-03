package com.jobtracker.backendJobTracker.status.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateStatusRequest {

    @NotBlank(message = "Status name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Color is required")
    @Size(max = 20)
    private String color;

    private boolean terminal;
}
