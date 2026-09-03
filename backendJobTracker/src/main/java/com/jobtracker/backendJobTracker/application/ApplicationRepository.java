package com.jobtracker.backendJobTracker.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jobtracker.backendJobTracker.application.enums.ContractType;
import com.jobtracker.backendJobTracker.application.enums.Seniority;
import com.jobtracker.backendJobTracker.application.enums.SourceBoard;
import com.jobtracker.backendJobTracker.application.enums.WorkMode;

public interface ApplicationRepository extends JpaRepository<Application, UUID>, JpaSpecificationExecutor<Application> {

    Optional<Application> findByIdAndUserId(UUID id, UUID userId);

    Page<Application> findByUserIdAndArchivedFalse(UUID userId, Pageable pageable);

    List<Application> findByUserIdAndContractType(UUID userId, ContractType contractType);

    List<Application> findByUserIdAndSeniority(UUID userId, Seniority seniority);

    List<Application> findByUserIdAndWorkMode(UUID userId, WorkMode workMode);

    List<Application> findByUserIdAndSourceBoard(UUID userId, SourceBoard sourceBoard);

    // ─── custom statuses support ───
    long countByStatusId(UUID statusId);

    @Modifying
    @Query("update Application a set a.status.id = :target where a.status.id = :from")
    void reassignStatus(@Param("from") UUID from, @Param("target") UUID target);
}
