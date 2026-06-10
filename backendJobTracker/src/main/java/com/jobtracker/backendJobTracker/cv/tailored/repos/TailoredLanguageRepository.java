package com.jobtracker.backendJobTracker.cv.tailored.repos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredLanguage;


public interface TailoredLanguageRepository extends JpaRepository<TailoredLanguage, UUID> {
    Optional<TailoredLanguage> findByIdAndTailoredCv_Application_User_Id(UUID id, UUID userId);
 
    // OK — був List. Параметр camelCase.
    List<TailoredLanguage> findByTailoredCv_Id(UUID tailoredCvId);

}
