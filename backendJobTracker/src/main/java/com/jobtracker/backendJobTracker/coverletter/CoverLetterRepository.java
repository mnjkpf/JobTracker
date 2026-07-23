package com.jobtracker.backendJobTracker.coverletter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CoverLetterRepository extends JpaRepository<CoverLetter, UUID> {
 
    
    List<CoverLetter> findByApplicationIdOrderByVersionDesc(UUID applicationId);
 
    
    Optional<CoverLetter> findByIdAndApplication_User_Id(UUID coverLetterId, UUID userId);
 
   
    Optional<CoverLetter> findTopByApplicationIdOrderByVersionDesc(UUID applicationId);
 
    
    void deleteByApplicationId(UUID applicationId);
}

