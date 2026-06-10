package com.jobtracker.backendJobTracker.cv.tailored.repos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredExperience;

public interface TailoredExperienceRepository extends JpaRepository<TailoredExperience, UUID> {
    // ВИПРАВЛЕНО: tenant-safe через graph: experience → tailoredCv → application → user.
    // Раніше було findByIdAndTailoredCv_Id — не tenant-safe.
    Optional<TailoredExperience> findByIdAndTailoredCv_Application_User_Id(UUID id, UUID userId);
 
    // ВИПРАВЛЕНО: був Optional, але один tailored CV має багато experiences → треба List.
    // Плюс ORDER BY startDate DESC — щоб service міг збирати CV у правильному порядку.
    List<TailoredExperience> findByTailoredCv_IdOrderByStartDateDesc(UUID tailoredCvId);

}
