package com.jobtracker.backendJobTracker.interview.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.jobtracker.backendJobTracker.interview.enums.InterviewPrepStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InterviewPrepResponse {
    private UUID id;
 
    
    private UUID applicationId;
 
    private InterviewPrepStatus status;
    private String promptVersion;
    private Instant createdAt;
 
   
    private List<InterviewQuestionResponse> technicalQuestions;
    private List<InterviewQuestionResponse> behavioralQuestions;
    private List<InterviewQuestionResponse> questionsToAsk;
}
