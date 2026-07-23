package com.jobtracker.backendJobTracker.interview.notes;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterviewNoteRepository extends JpaRepository<InterviewNote, UUID> {

    
    List<InterviewNote> findByInterviewPrepIdOrderByCreatedAtDesc(UUID interviewPrepId);

    Optional<InterviewNote> findByIdAndInterviewPrep_Application_User_Id(UUID id, UUID userId);

    void deleteByInterviewPrepId(UUID interviewPrepId);

    
    @Query("SELECT n FROM InterviewNote n " +
           "WHERE n.interviewPrep.application.user.id = :userId " +
           "AND n.embedding IS NOT NULL")
    List<InterviewNote> findAllUserNotesWithEmbedding(@Param("userId") UUID userId);
}