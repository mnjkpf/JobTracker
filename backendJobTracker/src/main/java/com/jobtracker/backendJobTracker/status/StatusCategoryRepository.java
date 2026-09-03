package com.jobtracker.backendJobTracker.status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jobtracker.backendJobTracker.application.enums.ApplicationStatus;

public interface StatusCategoryRepository extends JpaRepository<StatusCategory, UUID> {

    List<StatusCategory> findByUserIdOrderByPositionAsc(UUID userId);

    Optional<StatusCategory> findByIdAndUserId(UUID id, UUID userId);

    Optional<StatusCategory> findByUserIdAndSystemType(UUID userId, ApplicationStatus systemType);

    boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name);

    long countByUserId(UUID userId);
}
