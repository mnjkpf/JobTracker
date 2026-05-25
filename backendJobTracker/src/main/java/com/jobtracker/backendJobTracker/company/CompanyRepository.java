package com.jobtracker.backendJobTracker.company;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jobtracker.backendJobTracker.user.User;


public interface CompanyRepository extends JpaRepository<Company, UUID> {
    Optional<Company> findByName(String name);

    List<Company> findByUser(User user);

    List<Company> findByIndustry(String industry);

    List<Company> findByCompanySize(CampanySize size);
}
