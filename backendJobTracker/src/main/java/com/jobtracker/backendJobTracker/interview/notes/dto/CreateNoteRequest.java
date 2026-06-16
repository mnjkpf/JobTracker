package com.jobtracker.backendJobTracker.interview.notes.dto;

import com.jobtracker.backendJobTracker.interview.notes.enums.NoteType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateNoteRequest {

    
    @NotBlank
    @Size(max = 5000)
    private String content;

    
    @NotNull
    private NoteType noteType;
}