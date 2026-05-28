package com.jobtracker.backendJobTracker.application;



import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobtracker.backendJobTracker.application.dto.ApplicationFilters;
import com.jobtracker.backendJobTracker.application.dto.ApplicationResponse;
import static com.jobtracker.backendJobTracker.application.dto.ApplicationSpecifications.appliedAfter;
import static com.jobtracker.backendJobTracker.application.dto.ApplicationSpecifications.appliedBefore;
import com.jobtracker.backendJobTracker.application.dto.ApplicationStatusHistoryResponse;

import static com.jobtracker.backendJobTracker.application.dto.ApplicationSpecifications.byCompany;
import static com.jobtracker.backendJobTracker.application.dto.ApplicationSpecifications.byContractType;
import static com.jobtracker.backendJobTracker.application.dto.ApplicationSpecifications.bySeniority;
import static com.jobtracker.backendJobTracker.application.dto.ApplicationSpecifications.bySourceBoard;
import static com.jobtracker.backendJobTracker.application.dto.ApplicationSpecifications.byStatus;
import static com.jobtracker.backendJobTracker.application.dto.ApplicationSpecifications.byUser;
import static com.jobtracker.backendJobTracker.application.dto.ApplicationSpecifications.byWorkMode;
import static com.jobtracker.backendJobTracker.application.dto.ApplicationSpecifications.minSalaryAtLeast;
import static com.jobtracker.backendJobTracker.application.dto.ApplicationSpecifications.notArchived;
import static com.jobtracker.backendJobTracker.application.dto.ApplicationSpecifications.searchQuery;
import com.jobtracker.backendJobTracker.application.dto.ApplicationSummaryResponse;
import com.jobtracker.backendJobTracker.application.dto.CreateApplicationRequest;
import com.jobtracker.backendJobTracker.application.dto.UpdateApplicationRequest;
import com.jobtracker.backendJobTracker.application.dto.UpdateStatusRequest;
import com.jobtracker.backendJobTracker.application.enums.ApplicationStatus;
import com.jobtracker.backendJobTracker.application.enums.SourceBoard;
import com.jobtracker.backendJobTracker.company.Company;
import com.jobtracker.backendJobTracker.company.CompanyService;
import com.jobtracker.backendJobTracker.exception.ConflictException;
import com.jobtracker.backendJobTracker.exception.ResourceNotFoundException;
import com.jobtracker.backendJobTracker.user.User;
import com.jobtracker.backendJobTracker.user.UserRepository;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository statusHistoryRepository;
    private final ApplicationStateMachine stateMachine;
    private final ApplicationMapper applicationMapper;
    private final CompanyService companyService;
    private final UserRepository userRepository;

    
    public ApplicationResponse getById(UUID userId, UUID id) {
        Application app = fetchOwned(userId, id);
        return applicationMapper.toResponse(app);
    }

    /**
     * List з динамічними фільтрами + пагінацією.
     * Specifications композитяться з ApplicationFilters; null filter = no-op.
     */
    public Page<ApplicationSummaryResponse> list(UUID userId, ApplicationFilters f, Pageable pageable) {
        Specification<Application> spec;
        spec = Specification.where(byUser(userId))
                .and(notArchived())
                .and(byStatus(f.getStatuses()))
                .and(byContractType(f.getContractType()))
                .and(bySeniority(f.getSeniorities()))
                .and(byWorkMode(f.getWorkMode()))
                .and(bySourceBoard(f.getSourceBoard()))
                .and(byCompany(f.getCompanyId()))
                .and(searchQuery(f.getQ()))
                .and(appliedAfter(f.getAppliedAfter()))
                .and(appliedBefore(f.getAppliedBefore()))
                .and(minSalaryAtLeast(f.getMinSalary()));

        return applicationRepository.findAll(spec, pageable)
                .map(applicationMapper::toApplicationSummaryResponse);
    }

    public List<ApplicationStatusHistoryResponse> getStatusHistory(UUID userId, UUID id) {
        // Tenant check: переконуємось що заявка належить юзеру, перш ніж віддати історію.
        fetchOwned(userId, id);
        return statusHistoryRepository.findByApplicationIdOrderByChangedAtAsc(id).stream()
                .map(applicationMapper::toStatusHistoryResponse)
                .toList();
    }

    
    @Transactional
    public ApplicationResponse create(UUID userId, CreateApplicationRequest request) {
        // Company через findOrCreate — уникаємо дублікатів компаній юзера.
        Company company = companyService.findOrCreate(userId, request.getCompanyName());

        // user через getReferenceById — lazy proxy, без зайвого SELECT.
        User userRef = userRepository.getReferenceById(userId);

        Application app = new Application();
        app.setUser(userRef);
        app.setCompany(company);
        app.setName(request.getName());
        app.setUrl(request.getUrl());
        app.setDescription(request.getDescription());
        app.setNotes(request.getNotes());
        app.setLocation(request.getLocation());
        app.setContractType(request.getContractType());
        app.setSeniority(request.getSeniority());
        app.setWorkMode(request.getWorkMode());
        app.setSalaryMin(request.getSalaryMin());
        app.setSalaryMax(request.getSalaryMax());
        app.setSalaryCurrency(request.getSalaryCurrency());
        // Manual creation → MANUAL source. (URL-based створення в 3B поставить інше.)
        app.setSourceBoard(SourceBoard.MANUAL);

        // Початковий статус: або заданий клієнтом, або SAVED.
        ApplicationStatus initial = request.getStatus() != null ? request.getStatus() : ApplicationStatus.SAVED;
        app.setStatus(initial);
        // Якщо юзер одразу створює як APPLIED — фіксуємо appliedAt.
        if (initial == ApplicationStatus.APPLIED) {
            app.setAppliedAt(Instant.now());
        }

        Application saved;
        try {
            saved = applicationRepository.save(app);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Composite unique (user_id, url) violated — заявка на цю URL вже є.
            throw new ConflictException("You already have an application for this URL");
        }

        // Перший запис історії: from=null → to=initial.
        writeHistory(saved, null, initial, "Application created");

        return applicationMapper.toResponse(saved);
    }

    
    /**
     * Часткове оновлення деталей. null поле = "не чіпати".
     * Статус НЕ міняється тут — окремий метод updateStatus.
     */
    @Transactional
    public ApplicationResponse updateDetails(UUID userId, UUID id, UpdateApplicationRequest r) {
        Application app = fetchOwned(userId, id);

        if (r.getName() != null) app.setName(r.getName());
        if (r.getDescription() != null) app.setDescription(r.getDescription());
        if (r.getNotes() != null) app.setNotes(r.getNotes());
        if (r.getLocation() != null) app.setLocation(r.getLocation());
        if (r.getContractType() != null) app.setContractType(r.getContractType());
        if (r.getSeniority() != null) app.setSeniority(r.getSeniority());
        if (r.getWorkMode() != null) app.setWorkMode(r.getWorkMode());
        if (r.getSalaryMin() != null) app.setSalaryMin(r.getSalaryMin());
        if (r.getSalaryMax() != null) app.setSalaryMax(r.getSalaryMax());
        if (r.getSalaryCurrency() != null) app.setSalaryCurrency(r.getSalaryCurrency());

        // save() не обов'язковий — entity у managed state, зміни flush'аться при commit.
        // Але явний save читабельніший і безпечніший при майбутніх рефакторах.
        return applicationMapper.toResponse(applicationRepository.save(app));
    }

    /**
     * Зміна статусу — найважливіший метод. State machine validation +
     * history запис + appliedAt при першому APPLIED. Все в одній транзакції.
     */
    @Transactional
    public ApplicationResponse updateStatus(UUID userId, UUID id, UpdateStatusRequest r) {
        Application app = fetchOwned(userId, id);

        ApplicationStatus from = app.getStatus();
        ApplicationStatus to = r.getStatus();

        // Кине BusinessRuleException → 422 якщо перехід недопустимий.
        stateMachine.validateTransition(from, to);

        app.setStatus(to);

        // appliedAt фіксується ОДИН раз — при першому переході в APPLIED.
        if (to == ApplicationStatus.APPLIED && app.getAppliedAt() == null) {
            app.setAppliedAt(Instant.now());
        }

        Application saved = applicationRepository.save(app);

        // Audit trail.
        writeHistory(saved, from, to, r.getNote());

        return applicationMapper.toResponse(saved);
    }



    /** Soft delete — приховує з list, зберігає в БД. Стандартний "delete" для юзера. */
    @Transactional
    public void archive(UUID userId, UUID id) {
        Application app = fetchOwned(userId, id);
        app.setArchived(true);
        applicationRepository.save(app);
    }

    /** Розархівувати. */
    @Transactional
    public void unarchive(UUID userId, UUID id) {
        Application app = fetchOwned(userId, id);
        app.setArchived(false);
        applicationRepository.save(app);
    }

    /**
     * Hard delete — фізично видаляє рядок + каскадно історію (ON DELETE CASCADE у БД).
     * Незворотно. Експонуємо обережно (можливо лише в admin/settings).
     */
    @Transactional
    public void hardDelete(UUID userId, UUID id) {
        Application app = fetchOwned(userId, id);
        applicationRepository.delete(app);
    }

    

    /** Tenant-safe fetch. Якщо заявки немає АБО вона чужа → 404. */
    private Application fetchOwned(UUID userId, UUID id) {
        return applicationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
    }

    private void writeHistory(Application app, ApplicationStatus from, ApplicationStatus to, String note) {
        ApplicationStatusHistory h = new ApplicationStatusHistory();
        h.setApplication(app);
        h.setFromStatus(from);   // null дозволено (перший запис)
        h.setToStatus(to);
        h.setNote(note);
        statusHistoryRepository.save(h);
    }
}