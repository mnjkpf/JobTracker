package com.jobtracker.backendJobTracker.interview;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.jobtracker.backendJobTracker.interview.dto.InterviewPrepResponse;
import com.jobtracker.backendJobTracker.interview.dto.InterviewPrepSummaryResponse;
import com.jobtracker.backendJobTracker.interview.dto.InterviewQuestionResponse;

@Mapper(componentModel = "spring")
public interface InterviewPrepMapper {
    
    @Mapping(target = "applicationId", source = "application.id")
    @Mapping(target = "technicalQuestions", ignore = true)
    @Mapping(target = "behavioralQuestions", ignore = true)
    @Mapping(target = "questionsToAsk", ignore = true)
    InterviewPrepResponse toInterviewPrepResponse(InterviewPrep interviewPrep);
 
    // ДОДАНО: applicationId маппінг + ignore questionCount (Service рахує)
    @Mapping(target = "applicationId", source = "application.id")
    @Mapping(target = "questionCount", ignore = true)
    InterviewPrepSummaryResponse toInterviewPrepSummaryResponse(InterviewPrep interviewPrep);
 
    InterviewQuestionResponse toInterviewQuestionResponse(InterviewQuestion interviewQuestion);

}
