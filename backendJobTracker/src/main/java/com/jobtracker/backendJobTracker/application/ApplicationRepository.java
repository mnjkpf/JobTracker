package com.jobtracker.backendJobTracker.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jobtracker.backendJobTracker.application.enums.ApplicationStatus;
import com.jobtracker.backendJobTracker.application.enums.ContractType;
import com.jobtracker.backendJobTracker.application.enums.Seniority;
import com.jobtracker.backendJobTracker.application.enums.SourceBoard;
import com.jobtracker.backendJobTracker.application.enums.WorkMode;
import com.jobtracker.backendJobTracker.company.Company;
import com.jobtracker.backendJobTracker.user.User;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    Optional<Application> findByName(String name);

    Optional<Application> findByCompany(Company company);

    Optional<Application> findByUser(User user);

    List<Application> findByApplicationStatus(ApplicationStatus applicationStatus);
    List<Application> findByContractType(ContractType contractType);
    List<Application> findBySeniority(Seniority seniority);
    List<Application> findByWorkMode(WorkMode workMode);
    List<Application> findBySourceBoard(SourceBoard sourceBoard);
}
