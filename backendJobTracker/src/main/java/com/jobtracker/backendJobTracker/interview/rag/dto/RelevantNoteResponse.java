package com.jobtracker.backendJobTracker.interview.rag.dto;

import java.util.UUID;

import com.jobtracker.backendJobTracker.interview.notes.enums.NoteType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RelevantNoteResponse {
 
    private UUID id;
    private String content;
    private NoteType noteType;
    /** З якої заявки нотатка — frontend показує "From [company]" badge. */
    private UUID applicationId;
    /** 0.0-1.0, для UI progress bar. */
    private Double similarity;
}
