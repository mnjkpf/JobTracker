package com.jobtracker.backendJobTracker.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jobtracker.backendJobTracker.application.dto.ApplicationResponse;
import com.jobtracker.backendJobTracker.application.dto.CreateApplicationRequest;
import com.jobtracker.backendJobTracker.application.dto.UpdateStatusRequest;
import com.jobtracker.backendJobTracker.application.enums.ApplicationStatus;
import com.jobtracker.backendJobTracker.application.enums.ContractType;
import com.jobtracker.backendJobTracker.application.enums.Seniority;
import com.jobtracker.backendJobTracker.application.enums.SourceBoard;
import com.jobtracker.backendJobTracker.application.enums.WorkMode;
import com.jobtracker.backendJobTracker.application.parsing.JobBoardDetector;
import com.jobtracker.backendJobTracker.application.parsing.JobPostingExtractionService;
import com.jobtracker.backendJobTracker.company.Company;
import com.jobtracker.backendJobTracker.company.CompanyService;
import com.jobtracker.backendJobTracker.exception.BusinessRuleException;
import com.jobtracker.backendJobTracker.exception.ResourceNotFoundException;
import com.jobtracker.backendJobTracker.user.User;
import com.jobtracker.backendJobTracker.user.UserRepository;

/**
 * Unit-тести для {@link ApplicationService} з Mockito — без БД і Spring контексту.
 * Перевіряємо ключову бізнес-логіку: company findOrCreate, MANUAL source,
 * запис history, appliedAt при APPLIED, валідація переходів, soft delete.
 */
@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private ApplicationStatusHistoryRepository statusHistoryRepository;
    @Mock private ApplicationStateMachine stateMachine;
    @Mock private ApplicationMapper applicationMapper;
    @Mock private CompanyService companyService;
    @Mock private UserRepository userRepository;
    @Mock private JobPostingExtractionService extractionService;
    @Mock private JobBoardDetector jobBoardDetector;

    @InjectMocks private ApplicationService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID appId = UUID.randomUUID();

    private CreateApplicationRequest validCreateRequest() {
        CreateApplicationRequest r = new CreateApplicationRequest();
        r.setName("Backend Engineer");
        r.setCompanyName("Acme");
        r.setUrl("https://example.com/job/1");
        r.setContractType(ContractType.B2B);
        r.setSeniority(Seniority.MID);
        r.setWorkMode(WorkMode.REMOTE);
        return r;
    }

    @Test
    @DisplayName("create: викликає findOrCreate, ставить MANUAL source, пише перший history (null->SAVED)")
    void create_manualSource_writesInitialHistory() {
        CreateApplicationRequest request = validCreateRequest();
        Company company = new Company();
        when(companyService.findOrCreate(userId, "Acme")).thenReturn(company);
        when(userRepository.getReferenceById(userId)).thenReturn(new User());
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(new ApplicationResponse());

        service.create(userId, request);

        verify(companyService).findOrCreate(userId, "Acme");

        ArgumentCaptor<Application> appCaptor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(appCaptor.capture());
        Application saved = appCaptor.getValue();
        assertThat(saved.getSourceBoard()).isEqualTo(SourceBoard.MANUAL);
        assertThat(saved.getStatus()).isEqualTo(ApplicationStatus.SAVED);
        assertThat(saved.getName()).isEqualTo("Backend Engineer");
        assertThat(saved.getUrl()).isEqualTo("https://example.com/job/1");
        assertThat(saved.getCompany()).isSameAs(company);
        assertThat(saved.getAppliedAt()).isNull();

        ArgumentCaptor<ApplicationStatusHistory> hCaptor = ArgumentCaptor.forClass(ApplicationStatusHistory.class);
        verify(statusHistoryRepository).save(hCaptor.capture());
        ApplicationStatusHistory history = hCaptor.getValue();
        assertThat(history.getFromStatus()).isNull();
        assertThat(history.getToStatus()).isEqualTo(ApplicationStatus.SAVED);
    }

    @Test
    @DisplayName("create зі статусом APPLIED ставить appliedAt і пише history null->APPLIED")
    void create_appliedStatus_setsAppliedAt() {
        CreateApplicationRequest request = validCreateRequest();
        request.setStatus(ApplicationStatus.APPLIED);
        when(companyService.findOrCreate(userId, "Acme")).thenReturn(new Company());
        when(userRepository.getReferenceById(userId)).thenReturn(new User());
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(new ApplicationResponse());

        service.create(userId, request);

        ArgumentCaptor<Application> appCaptor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(appCaptor.capture());
        assertThat(appCaptor.getValue().getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(appCaptor.getValue().getAppliedAt()).isNotNull();

        ArgumentCaptor<ApplicationStatusHistory> hCaptor = ArgumentCaptor.forClass(ApplicationStatusHistory.class);
        verify(statusHistoryRepository).save(hCaptor.capture());
        assertThat(hCaptor.getValue().getToStatus()).isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    @DisplayName("updateStatus: валідує перехід, ставить appliedAt при першому APPLIED, пише history")
    void updateStatus_validTransition() {
        Application app = new Application();
        app.setStatus(ApplicationStatus.SAVED);
        when(applicationRepository.findByIdAndUserId(appId, userId)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(new ApplicationResponse());

        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus(ApplicationStatus.APPLIED);
        request.setNote("Sent CV");

        service.updateStatus(userId, appId, request);

        verify(stateMachine).validateTransition(ApplicationStatus.SAVED, ApplicationStatus.APPLIED);
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(app.getAppliedAt()).isNotNull();

        ArgumentCaptor<ApplicationStatusHistory> hCaptor = ArgumentCaptor.forClass(ApplicationStatusHistory.class);
        verify(statusHistoryRepository).save(hCaptor.capture());
        ApplicationStatusHistory history = hCaptor.getValue();
        assertThat(history.getFromStatus()).isEqualTo(ApplicationStatus.SAVED);
        assertThat(history.getToStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(history.getNote()).isEqualTo("Sent CV");
    }

    @Test
    @DisplayName("updateStatus: недозволений перехід -> BusinessRuleException, нічого не зберігається")
    void updateStatus_invalidTransition() {
        Application app = new Application();
        app.setStatus(ApplicationStatus.SAVED);
        when(applicationRepository.findByIdAndUserId(appId, userId)).thenReturn(Optional.of(app));
        doThrow(new BusinessRuleException("Invalid status transition: SAVED → OFFER"))
                .when(stateMachine).validateTransition(ApplicationStatus.SAVED, ApplicationStatus.OFFER);

        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus(ApplicationStatus.OFFER);

        assertThatThrownBy(() -> service.updateStatus(userId, appId, request))
                .isInstanceOf(BusinessRuleException.class);

        verify(applicationRepository, never()).save(any());
        verify(statusHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("archive: soft delete ставить archived=true і зберігає")
    void archive_setsArchivedTrue() {
        Application app = new Application();
        app.setArchived(false);
        when(applicationRepository.findByIdAndUserId(appId, userId)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        service.archive(userId, appId);

        assertThat(app.isArchived()).isTrue();
        verify(applicationRepository).save(app);
    }

    @Test
    @DisplayName("getById: знайдено -> мапиться у ApplicationResponse")
    void getById_found() {
        Application app = new Application();
        ApplicationResponse expected = new ApplicationResponse();
        when(applicationRepository.findByIdAndUserId(appId, userId)).thenReturn(Optional.of(app));
        when(applicationMapper.toResponse(app)).thenReturn(expected);

        assertThat(service.getById(userId, appId)).isSameAs(expected);
    }

    @Test
    @DisplayName("getById: чужа/відсутня заявка -> ResourceNotFoundException (tenant isolation)")
    void getById_notFound() {
        when(applicationRepository.findByIdAndUserId(eq(appId), eq(userId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(userId, appId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
