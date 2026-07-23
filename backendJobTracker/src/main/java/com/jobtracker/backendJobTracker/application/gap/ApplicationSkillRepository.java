package com.jobtracker.backendJobTracker.application.gap;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationSkillRepository extends JpaRepository<ApplicationSkill, java.util.UUID> {
    List<ApplicationSkill> findByApplicationId(UUID applicationId);
    Optional<ApplicationSkill> findByApplicationIdAndSkillId(UUID applicationId, UUID skillId);
    void deleteByApplicationId(UUID applicationId);
}
