package com.jobtracker.backendJobTracker.cv.tailored.repos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredSkill;

public interface TailoredSkillRepository extends JpaRepository<TailoredSkill, UUID> {
    // ВИПРАВЛЕНО: tenant-safe graph навігація.
    Optional<TailoredSkill> findByIdAndTailoredCv_Application_User_Id(UUID id, UUID userId);
 
    // ДОДАНО: для збору CV в service.
    List<TailoredSkill> findByTailoredCv_Id(UUID tailoredCvId);

}
