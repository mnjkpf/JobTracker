package com.jobtracker.backendJobTracker.cv.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jobtracker.backendJobTracker.cv.models.MasterCvSkill;

public interface MasterCvSkillRepository extends JpaRepository<MasterCvSkill, UUID> {
    Optional<MasterCvSkill> findByIdAndMasterCv_User_Id(UUID id, UUID userId);
 
    // Чи цей юзер уже має цей скіл (для duplicate check перед addSkill)
    Optional<MasterCvSkill> findByMasterCvIdAndSkillId(UUID masterCvId, UUID skillId);
 
    // Усі скіли мого CV — для збору full CV у CvService
    List<MasterCvSkill> findByMasterCvId(UUID masterCvId);


    
}
