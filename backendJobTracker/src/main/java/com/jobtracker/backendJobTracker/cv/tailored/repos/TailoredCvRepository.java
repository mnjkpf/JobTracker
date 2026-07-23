package com.jobtracker.backendJobTracker.cv.tailored.repos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredCv;

public interface TailoredCvRepository extends JpaRepository<TailoredCv, UUID> {
    Optional<TailoredCv> findByIdAndApplication_User_Id(UUID id, UUID userId);
 
    // Усі версії заявки (для list endpoint). List, не Optional.
    List<TailoredCv> findByApplication_IdOrderByVersionDesc(UUID applicationId);
 
    // ДОДАНО: latest version — для nextVersion() розрахунку.
    Optional<TailoredCv> findTopByApplication_IdOrderByVersionDesc(UUID applicationId);

}
