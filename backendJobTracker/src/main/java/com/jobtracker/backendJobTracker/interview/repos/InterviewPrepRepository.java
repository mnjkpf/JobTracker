package com.jobtracker.backendJobTracker.interview.repos;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jobtracker.backendJobTracker.interview.InterviewPrep;

public interface InterviewPrepRepository extends JpaRepository<InterviewPrep, UUID> {
    Optional<InterviewPrep> findByApplicationId(UUID applicationId);

    Optional<InterviewPrep> findByIdAndApplication_User_Id(UUID id, UUID userId);

    boolean existsByApplicationId(UUID applicationId);

    void deleteByApplicationId(UUID applicationId);
}
