package com.jobtracker.backendJobTracker.cv.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jobtracker.backendJobTracker.cv.models.Language;

public interface LanguageRepository extends JpaRepository<Language, UUID> {
    Optional<Language> findByIdAndMasterCv_User_Id(UUID id, UUID userId);
 
    
 
    List<Language> findByMasterCvId(UUID masterCvId);

}
