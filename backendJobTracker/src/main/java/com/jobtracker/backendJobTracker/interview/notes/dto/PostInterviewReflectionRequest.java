package com.jobtracker.backendJobTracker.interview.notes.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostInterviewReflectionRequest {

    @NotBlank
    @Size(max = 10000)
    private String content;

    
    @Size(max = 50)
    private List<String> questionsAsked;
}