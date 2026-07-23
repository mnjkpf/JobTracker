package com.jobtracker.backendJobTracker.interview.repos;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jobtracker.backendJobTracker.interview.InterviewQuestion;
import com.jobtracker.backendJobTracker.interview.enums.QuestionCategory;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, UUID> {
    List<InterviewQuestion> findByInterviewPrepIdOrderByDisplayOrderAsc(UUID interviewPrepId);

    List<InterviewQuestion> findByInterviewPrepIdAndCategory(UUID interviewPrepId, QuestionCategory category);

    void deleteByInterviewPrepId(UUID interviewPrepId);
}