package com.jobtracker.backendJobTracker.interview.notes.dto;

import java.time.Instant;
import java.util.UUID;

import com.jobtracker.backendJobTracker.interview.notes.enums.NoteType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InterviewNoteResponse {

    private UUID id;
    
    private UUID interviewPrepId;
   
    private String content;
    private NoteType noteType;
    private Instant createdAt;
    private Instant updatedAt;
}