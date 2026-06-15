package com.jobtracker.backendJobTracker.interview.dto;

import java.util.UUID;

import com.jobtracker.backendJobTracker.interview.enums.QuestionCategory;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class InterviewQuestionResponse {
    

    private UUID id;
    private QuestionCategory category;
    private String question;
    private String suggestedAnswer;
    private Integer displayOrder;
}
