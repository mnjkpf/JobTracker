package com.jobtracker.backendJobTracker.cv.tailored.repos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredEducation;

public interface TailoredEducationRepository extends JpaRepository<TailoredEducation, UUID> {
    Optional<TailoredEducation> findByIdAndTailoredCv_Application_User_Id(UUID id, UUID userId);
 
    // ВИПРАВЛЕНО: був Optional — багато educations → List + порядок.
    List<TailoredEducation> findByTailoredCv_IdOrderByStartDateDesc(UUID tailoredCvId);

}
