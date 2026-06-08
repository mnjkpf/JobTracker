package com.jobtracker.backendJobTracker.cv.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jobtracker.backendJobTracker.cv.models.Education;

public interface EducationRepository extends JpaRepository<Education, UUID> {
 
    Optional<Education> findByIdAndMasterCv_User_Id(UUID id, UUID userId);
 
    List<Education> findByMasterCvIdOrderByStartDateDesc(UUID masterCvId);
}

