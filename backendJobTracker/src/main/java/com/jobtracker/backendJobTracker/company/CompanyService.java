package com.jobtracker.backendJobTracker.company;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobtracker.backendJobTracker.company.dto.CompanyResponse;
import com.jobtracker.backendJobTracker.company.dto.CreateCompanyRequest;
import com.jobtracker.backendJobTracker.exception.ConflictException;
import com.jobtracker.backendJobTracker.exception.ResourceNotFoundException;
import com.jobtracker.backendJobTracker.user.User;
import com.jobtracker.backendJobTracker.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final CompanyMapper companyMapper;   // ЗМІНЕНО: замість inline toResponse

    /**
     * Знайти існуючу компанію або створити нову. Повертає Company entity
     * (не DTO) — щоб ApplicationService міг application.setCompany(c).
     */
    @Transactional
    public Company findOrCreate(UUID userId, String name) {
        return companyRepository.findByUserIdAndName(userId, name)
                .orElseGet(() -> {
                    Company c = new Company();
                    c.setName(name);
                    User userRef = userRepository.getReferenceById(userId);
                    c.setUser(userRef);
                    return companyRepository.save(c);
                });
    }

    @Transactional
    public CompanyResponse create(UUID userId, CreateCompanyRequest request) {
        if (companyRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new ConflictException("Company '" + request.getName() + "' already exists");
        }

        Company c = new Company();
        c.setName(request.getName());
        c.setWebsite(request.getWebsite());
        c.setIndustry(request.getIndustry());
        c.setDescription(request.getDescription());
        c.setSize(request.getSize());
        c.setUser(userRepository.getReferenceById(userId));

        return companyMapper.toResponse(companyRepository.save(c));
    }

    public CompanyResponse getById(UUID userId, UUID companyId) {
        Company c = companyRepository.findByIdAndUserId(companyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));
        return companyMapper.toResponse(c);
    }

    public List<CompanyResponse> getAll(UUID userId) {
        return companyRepository.findByUserId(userId).stream()
                .map(companyMapper::toResponse)
                .toList();
    }

    public List<CompanyResponse> getByIndustry(UUID userId, String industry) {
        return companyRepository.findByUserIdAndIndustry(userId, industry).stream()
                .map(companyMapper::toResponse)
                .toList();
    }

    public List<CompanyResponse> getBySize(UUID userId, CompanySize size) {
        return companyRepository.findByUserIdAndSize(userId, size).stream()
                .map(companyMapper::toResponse)
                .toList();
    }

    @Transactional
    public CompanyResponse update(UUID userId, UUID companyId, CreateCompanyRequest request) {
        Company c = companyRepository.findByIdAndUserId(companyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));

        if (!c.getName().equals(request.getName())
                && companyRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new ConflictException("Company '" + request.getName() + "' already exists");
        }

        c.setName(request.getName());
        c.setWebsite(request.getWebsite());
        c.setIndustry(request.getIndustry());
        c.setDescription(request.getDescription());
        c.setSize(request.getSize());
        return companyMapper.toResponse(companyRepository.save(c));
    }

    @Transactional
    public void delete(UUID userId, UUID companyId) {
        Company c = companyRepository.findByIdAndUserId(companyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));
        companyRepository.delete(c);
        // Якщо є Applications прив'язані → DataIntegrityViolationException через FK.
        // GlobalExceptionHandler конвертує. У майбутньому — soft delete для Company теж.
    }
}