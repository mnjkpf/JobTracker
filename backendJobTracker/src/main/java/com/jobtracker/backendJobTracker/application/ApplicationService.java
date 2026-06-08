package com.jobtracker.backendJobTracker.application;



import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
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
import com.jobtracker.backendJobTracker.application.enums.ContractType;
import com.jobtracker.backendJobTracker.application.enums.Seniority;
import com.jobtracker.backendJobTracker.application.enums.SourceBoard;
import com.jobtracker.backendJobTracker.application.enums.WorkMode;
import com.jobtracker.backendJobTracker.application.parsing.JobBoardDetector;
import com.jobtracker.backendJobTracker.application.parsing.JobPostingExtractionService;
import com.jobtracker.backendJobTracker.application.parsing.dto.ParseUrlPreviewResponse;
import com.jobtracker.backendJobTracker.application.parsing.dto.ParsedJobPosting;
import com.jobtracker.backendJobTracker.company.Company;
import com.jobtracker.backendJobTracker.company.CompanyService;
import com.jobtracker.backendJobTracker.exception.BusinessRuleException;
import com.jobtracker.backendJobTracker.exception.ConflictException;
import com.jobtracker.backendJobTracker.exception.ResourceNotFoundException;
import com.jobtracker.backendJobTracker.user.User;
import com.jobtracker.backendJobTracker.user.UserRepository;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationService {
 
    // ДОДАНО: константи для history notes — консистентні рядки замість magic strings.
    // Спрощує analytics ("скільки заявок створено через URL").
    private static final String NOTE_CREATED_MANUAL = "Application created manually";
    private static final String NOTE_CREATED_FROM_URL = "Application created from parsed URL";
 
    private final ApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository statusHistoryRepository;
    private final ApplicationStateMachine stateMachine;
    private final ApplicationMapper applicationMapper;
    private final CompanyService companyService;
    private final UserRepository userRepository;
    // ДОДАНО для URL-based flow
    private final JobPostingExtractionService extractionService;
    private final JobBoardDetector jobBoardDetector;
 
    // ═══════════════════════════════════════════════════════════════
    // READ
    // ═══════════════════════════════════════════════════════════════
 
    public ApplicationResponse getById(UUID userId, UUID id) {
        return applicationMapper.toResponse(fetchOwned(userId, id));
    }
 
    public Page<ApplicationSummaryResponse> list(UUID userId, ApplicationFilters f, Pageable pageable) {
        Specification<Application> spec = Specification.where(byUser(userId))
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
        fetchOwned(userId, id);  // tenant check
        return statusHistoryRepository.findByApplicationIdOrderByChangedAtAsc(id).stream()
                .map(applicationMapper::toStatusHistoryResponse)
                .toList();
    }
 
    // ═══════════════════════════════════════════════════════════════
    // CREATE — MANUAL
    // ═══════════════════════════════════════════════════════════════
 
    @Transactional
    public ApplicationResponse create(UUID userId, CreateApplicationRequest request) {
        Company company = companyService.findOrCreate(userId, request.getCompanyName());
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
        app.setSourceBoard(SourceBoard.MANUAL);
 
        ApplicationStatus initial = request.getStatus() != null ? request.getStatus() : ApplicationStatus.SAVED;
        app.setStatus(initial);
        if (initial == ApplicationStatus.APPLIED) {
            app.setAppliedAt(Instant.now());
        }
 
        Application saved = saveOrThrowDuplicate(app);
        writeHistory(saved, null, initial, NOTE_CREATED_MANUAL);
        return applicationMapper.toResponse(saved);
    }
 
    // ═══════════════════════════════════════════════════════════════
    // URL-BASED — PREVIEW (без save)
    // ═══════════════════════════════════════════════════════════════
 
    /**
     * Парсить URL, повертає попередній перегляд. НІЧОГО не зберігає.
     * <p>
     * userId не приймається — preview ні від кого не залежить, юзер не потрібен.
     * Однак ставимо @Transactional(readOnly) бо в майбутньому можливо логуватимемо
     * парсинги per-user (для cost tracking).
     */
    public ParseUrlPreviewResponse previewFromUrl(String url) {
        ParsedJobPosting parsed = extractionService.parse(url)
                .orElseThrow(() -> new BusinessRuleException(
                        "Could not extract job posting data from this URL. Please add the application manually."));
 
        ParseUrlPreviewResponse response = new ParseUrlPreviewResponse();
        response.setUrl(url);
        response.setPosition(parsed.getPosition());
        response.setCompanyName(parsed.getCompanyName());
        response.setDescription(parsed.getDescription());
        response.setLocation(parsed.getLocation());
        response.setSalaryMin(parsed.getSalaryMin());
        response.setSalaryMax(parsed.getSalaryMax());
        response.setSalaryCurrency(parsed.getSalaryCurrency());
        response.setContractType(parsed.getContractType());
        response.setSeniority(parsed.getSeniority());
        response.setWorkMode(parsed.getWorkMode());
        response.setSourceBoard(jobBoardDetector.detect(url));
        response.setParsedByLlm(parsed.isParsedByLlm());
        response.setHint(parsed.isParsedByLlm()
                ? "AI extracted these details. Please review carefully — salary and seniority may need correction."
                : "Details extracted automatically. Please verify before saving.");
        return response;
    }
 
    // ═══════════════════════════════════════════════════════════════
    // URL-BASED — CREATE (parse + persist в одній операції)
    // ═══════════════════════════════════════════════════════════════
 
    /**
     * Парсить URL і одразу створює заявку. Для випадків коли юзер довіряє
     * AI/parser і не хоче проміжного preview-кроку.
     */
    @Transactional
    public ApplicationResponse createFromUrl(UUID userId, String url) {
        ParsedJobPosting parsed = extractionService.parse(url)
                .orElseThrow(() -> new BusinessRuleException(
                        "Could not extract job posting data from this URL. Please add the application manually."));
 
        Company company = companyService.findOrCreate(userId, parsed.getCompanyName());
        User userRef = userRepository.getReferenceById(userId);
 
        Application app = new Application();
        app.setUser(userRef);
        app.setCompany(company);
        app.setName(parsed.getPosition());
        app.setUrl(url);
        app.setDescription(parsed.getDescription());
        app.setLocation(parsed.getLocation());
 
        // Defaults для NOT NULL полів коли парсер не визначив.
        // Тепер всі три enum мають NOT_SPECIFIED — consistent behavior, без оманливого MID.
        app.setContractType(parsed.getContractType() != null ? parsed.getContractType() : ContractType.NOT_SPECIFIED);
        app.setSeniority(parsed.getSeniority() != null ? parsed.getSeniority() : Seniority.NOT_SPECIFIED);
        app.setWorkMode(parsed.getWorkMode() != null ? parsed.getWorkMode() : WorkMode.NOT_SPECIFIED);
 
        applySalary(app, parsed);
 
        app.setSourceBoard(jobBoardDetector.detect(url));
        app.setStatus(ApplicationStatus.SAVED);
 
        Application saved = saveOrThrowDuplicate(app);
        writeHistory(saved, null, ApplicationStatus.SAVED, NOTE_CREATED_FROM_URL);
        return applicationMapper.toResponse(saved);
    }
 
    // ═══════════════════════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════════════════════
 
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
 
        return applicationMapper.toResponse(applicationRepository.save(app));
    }
 
    @Transactional
    public ApplicationResponse updateStatus(UUID userId, UUID id, UpdateStatusRequest r) {
        Application app = fetchOwned(userId, id);
 
        ApplicationStatus from = app.getStatus();
        ApplicationStatus to = r.getStatus();
 
        stateMachine.validateTransition(from, to);
 
        app.setStatus(to);
        if (to == ApplicationStatus.APPLIED && app.getAppliedAt() == null) {
            app.setAppliedAt(Instant.now());
        }
 
        Application saved = applicationRepository.save(app);
        writeHistory(saved, from, to, r.getNote());
        return applicationMapper.toResponse(saved);
    }
 
    // ═══════════════════════════════════════════════════════════════
    // DELETE / ARCHIVE
    // ═══════════════════════════════════════════════════════════════
 
    @Transactional
    public void archive(UUID userId, UUID id) {
        Application app = fetchOwned(userId, id);
        app.setArchived(true);
        applicationRepository.save(app);
    }
 
    @Transactional
    public void unarchive(UUID userId, UUID id) {
        Application app = fetchOwned(userId, id);
        app.setArchived(false);
        applicationRepository.save(app);
    }
 
    @Transactional
    public void hardDelete(UUID userId, UUID id) {
        applicationRepository.delete(fetchOwned(userId, id));
    }
 
    // ═══════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════
 
    private Application fetchOwned(UUID userId, UUID id) {
        return applicationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
    }
 
    private Application saveOrThrowDuplicate(Application app) {
        try {
            return applicationRepository.save(app);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("You already have an application for this URL");
        }
    }
 
    private void writeHistory(Application app, ApplicationStatus from, ApplicationStatus to, String note) {
        ApplicationStatusHistory h = new ApplicationStatusHistory();
        h.setApplication(app);
        h.setFromStatus(from);
        h.setToStatus(to);
        h.setNote(note);
        statusHistoryRepository.save(h);
    }
 
    /**
     * Salary з парсера ненадійний — буває лише min, лише max, або інвертований діапазон.
     * БД має CHECK (salary_min <= salary_max), тож інвертований діапазон відкидаємо повністю,
     * щоб не отримати оманливий ConflictException замість чесного "не розпарсилось".
     */
    private void applySalary(Application app, ParsedJobPosting parsed) {
        Integer min = parsed.getSalaryMin();
        Integer max = parsed.getSalaryMax();
        if (min != null && max != null && min > max) {
            return;
        }
        app.setSalaryMin(min);
        app.setSalaryMax(max);
        app.setSalaryCurrency(parsed.getSalaryCurrency());
    }
}
