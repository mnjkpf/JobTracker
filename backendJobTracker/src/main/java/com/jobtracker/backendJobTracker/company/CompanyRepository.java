package com.jobtracker.backendJobTracker.company;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    // ВИПРАВЛЕНО: було findByName(name) — глобально, multi-tenancy violation.
    // Тепер фільтрує по userId — кожен юзер у власному namespace.
    Optional<Company> findByUserIdAndName(UUID userId, String name);

    // ДОДАНО: tenant-safe single fetch для GET /companies/{id}.
    Optional<Company> findByIdAndUserId(UUID id, UUID userId);

    // ВИПРАВЛЕНО: було findByUser(User user) — потребує спочатку завантажити
    // User entity з БД (extra SELECT). findByUserId(UUID) робить це одним JOIN.
    List<Company> findByUserId(UUID userId);

    List<Company> findByUserIdAndIndustry(UUID userId, String industry);

    // ВИПРАВЛЕНО: було findByUserIdAndCompanySize — поле називається size,
    // не companySize. Spring шукав би неіснуюче поле → startup fail.
    List<Company> findByUserIdAndSize(UUID userId, CompanySize size);

    // ДОДАНО: для unique check у CompanyService.create
    boolean existsByUserIdAndName(UUID userId, String name);
}