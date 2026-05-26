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

    /**
     * Знайти існуючу компанію або створити нову.
     * Викликається з ApplicationService при парсингу job board.
     * <p>
     * ВИПРАВЛЕНО: повертає Company entity, не CompanyResponse — щоб ApplicationService
     * міг встановити application.setCompany(c). DTO для controller-шару, entity для service-шару.
     */
    @Transactional
    public Company findOrCreate(UUID userId, String name) {
        return companyRepository.findByUserIdAndName(userId, name)
                .orElseGet(() -> {
                    Company c = new Company();
                    c.setName(name);
                    // getReferenceById — lazy proxy, не робить SELECT.
                    // Просто прив'язує FK; ми вже знаємо що user існує (із JWT context).
                    User userRef = userRepository.getReferenceById(userId);
                    c.setUser(userRef);
                    return companyRepository.save(c);
                });
    }

    @Transactional
    public CompanyResponse create(UUID userId, CreateCompanyRequest request) {
        // ВИПРАВЛЕНО: перевірка ПЕРЕД build, через existsBy (efficient SELECT EXISTS).
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

        return toResponse(companyRepository.save(c));
    }

    /**
     * Single fetch. ВИПРАВЛЕНО: було повернення null при not-found. Тепер throw —
     * GlobalExceptionHandler конвертує у 404 ProblemDetail.
     */
    public CompanyResponse getById(UUID userId, UUID companyId) {
        Company c = companyRepository.findByIdAndUserId(companyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));
        return toResponse(c);
    }

    public List<CompanyResponse> getAll(UUID userId) {
        return companyRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<CompanyResponse> getByIndustry(UUID userId, String industry) {
        return companyRepository.findByUserIdAndIndustry(userId, industry).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<CompanyResponse> getBySize(UUID userId, CompanySize size) {
        return companyRepository.findByUserIdAndSize(userId, size).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CompanyResponse update(UUID userId, UUID companyId, CreateCompanyRequest request) {
        Company c = companyRepository.findByIdAndUserId(companyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));

        // Якщо змінюється name — перевір що нове ім'я не конфліктує
        if (!c.getName().equals(request.getName())
                && companyRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new ConflictException("Company '" + request.getName() + "' already exists");
        }

        c.setName(request.getName());
        c.setWebsite(request.getWebsite());
        c.setIndustry(request.getIndustry());
        c.setDescription(request.getDescription());
        c.setSize(request.getSize());
        return toResponse(companyRepository.save(c));
    }

    @Transactional
    public void delete(UUID userId, UUID companyId) {
        Company c = companyRepository.findByIdAndUserId(companyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));
        companyRepository.delete(c);
        // ПРИМІТКА: Якщо є Applications прив'язані до цієї Company — DELETE впаде
        // через FK constraint. У майбутньому додамо soft delete або cascade — поки що
        // raw delete і помилка від БД (ConflictException у GlobalExceptionHandler).
    }

    private CompanyResponse toResponse(Company c) {
        CompanyResponse r = new CompanyResponse();
        r.setId(c.getId());
        r.setCompanyName(c.getName());
        r.setWebsite(c.getWebsite());
        r.setIndustry(c.getIndustry());
        r.setDescription(c.getDescription());
        r.setSize(c.getSize());
        return r;
    }
}