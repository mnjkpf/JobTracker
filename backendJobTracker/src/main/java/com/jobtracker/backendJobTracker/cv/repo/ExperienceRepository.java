package com.jobtracker.backendJobTracker.cv.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jobtracker.backendJobTracker.cv.models.Experience;

public interface ExperienceRepository extends JpaRepository<Experience, UUID>{
    Optional<Experience> findByIdAndMasterCv_User_Id(UUID id, UUID userId);
 
    // усі досвіди CV, від найновішого. CvService викликає коли збирає full CV.
    List<Experience> findByMasterCvIdOrderByStartDateDesc(UUID masterCvId);
}


