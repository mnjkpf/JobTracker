package com.jobtracker.backendJobTracker.interview.notes.dto;

import java.util.UUID;

import com.jobtracker.backendJobTracker.interview.notes.enums.NoteType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimilarInterviewNote {

    private UUID id;
    
    private String content;
    private NoteType noteType;
   
    private UUID applicationId;
    private Double similarity;
}