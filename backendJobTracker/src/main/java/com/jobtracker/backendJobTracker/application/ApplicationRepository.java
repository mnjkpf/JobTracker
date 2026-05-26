package com.jobtracker.backendJobTracker.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jobtracker.backendJobTracker.application.enums.ApplicationStatus;
import com.jobtracker.backendJobTracker.application.enums.ContractType;
import com.jobtracker.backendJobTracker.application.enums.Seniority;
import com.jobtracker.backendJobTracker.application.enums.SourceBoard;
import com.jobtracker.backendJobTracker.application.enums.WorkMode;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    // ДОДАНО: tenant-safe single fetch. Не використовуйте просто findById —
    // це поверне Application іншого юзера, якщо хтось вгадає UUID.
    Optional<Application> findByIdAndUserId(UUID id, UUID userId);

    // ДОДАНО: list endpoint з пагінацією. archived=false фільтрує soft-deleted.
    Page<Application> findByUserIdAndArchivedFalse(UUID userId, Pageable pageable);

    // ВИПРАВЛЕНО: було findByUserIdAndApplicationStatus — Spring шукав поле
    // applicationStatus, якого немає. Поле називається status.
    List<Application> findByUserIdAndStatus(UUID userId, ApplicationStatus status);

    List<Application> findByUserIdAndContractType(UUID userId, ContractType contractType);

    List<Application> findByUserIdAndSeniority(UUID userId, Seniority seniority);

    List<Application> findByUserIdAndWorkMode(UUID userId, WorkMode workMode);

    List<Application> findByUserIdAndSourceBoard(UUID userId, SourceBoard sourceBoard);

    // Примітка: всі finder'и вище — тимчасові. У наступній ітерації 3A
    // замінимо на Specifications для composable filtering. Список фільтрів
    // зростатиме (status + workMode + seniority + searchQuery + ...) — і
    // методів буде 2^N. Specifications уникають цього.
}