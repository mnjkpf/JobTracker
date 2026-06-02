package com.jobtracker.backendJobTracker.cv.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jobtracker.backendJobTracker.cv.models.Project;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
 
    Optional<Project> findByIdAndMasterCv_User_Id(UUID id, UUID userId);
 
    List<Project> findByMasterCvIdOrderByStartDateDesc(UUID masterCvId);
}

