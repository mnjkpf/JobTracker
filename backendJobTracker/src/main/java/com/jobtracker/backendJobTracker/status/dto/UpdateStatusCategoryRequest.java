package com.jobtracker.backendJobTracker.status.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateStatusCategoryRequest {

    @Size(max = 100)
    private String name;

    @Size(max = 20)
    private String color;

    private Boolean terminal;
}
