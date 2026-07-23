package com.jobtracker.backendJobTracker.cv.tailored.repos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredProject;

public interface TailoredProjectRepository extends JpaRepository<TailoredProject, UUID> {
    

    Optional<TailoredProject> findByIdAndTailoredCv_Application_User_Id(UUID id, UUID userId);
 
    // ВИПРАВЛЕНО: був Optional — багато проектів → List + порядок.
    List<TailoredProject> findByTailoredCv_IdOrderByStartDateDesc(UUID tailoredCvId);

}
