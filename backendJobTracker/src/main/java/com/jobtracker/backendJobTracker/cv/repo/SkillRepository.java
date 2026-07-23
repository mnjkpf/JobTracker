package com.jobtracker.backendJobTracker.cv.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jobtracker.backendJobTracker.cv.models.Skill;

public interface SkillRepository extends JpaRepository<Skill, UUID> {
 
    // Lookup за normalized name. Service нормалізує до lowercase перед запитом.
    Optional<Skill> findByName(String name);
 
    boolean existsByName(String name);
}

