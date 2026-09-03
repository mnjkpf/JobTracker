package com.jobtracker.backendJobTracker.cv.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.jobtracker.backendJobTracker.cv.dto.ApplicationCvMeta;
import com.jobtracker.backendJobTracker.cv.models.ApplicationCv;

public interface ApplicationCvRepository extends JpaRepository<ApplicationCv, UUID> {

    Optional<ApplicationCv> findByApplicationId(UUID applicationId);
    boolean existsByApplicationId(UUID applicationId);
    void deleteByApplicationId(UUID applicationId);

    // Lightweight projection without the bytes — for metadata and AI context.
    @Query("""
        select c.id as id, c.fileName as fileName, c.contentType as contentType,
               c.fileSize as fileSize, c.extractedText as extractedText,
               c.createdAt as createdAt
        from ApplicationCv c
        where c.applicationId = :applicationId
        """)
    Optional<ApplicationCvMeta> findMetaByApplicationId(UUID applicationId);
}
