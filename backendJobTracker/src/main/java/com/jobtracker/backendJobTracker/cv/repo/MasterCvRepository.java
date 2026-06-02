package com.jobtracker.backendJobTracker.cv.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jobtracker.backendJobTracker.cv.models.MasterCv;

public interface MasterCvRepository extends JpaRepository<MasterCv, UUID> {
    Optional<MasterCv> findByUserId(UUID userId);
 
    // Для tenant-safe single fetch
    Optional<MasterCv> findByIdAndUserId(UUID id, UUID userId);
 
    boolean existsByUserId(UUID userId);



}
