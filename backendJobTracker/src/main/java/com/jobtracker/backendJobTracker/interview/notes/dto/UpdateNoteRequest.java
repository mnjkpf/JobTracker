package com.jobtracker.backendJobTracker.interview.notes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateNoteRequest {
    @NotBlank
    @Size(max = 5000)
    private String content;
}
