package com.jobtracker.backendJobTracker.application.parsing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParseUrlRequest {

    
    @NotBlank(message = "URL is required")
    @Size(max = 2048)
    private String url;


}
