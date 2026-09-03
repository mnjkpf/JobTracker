package com.jobtracker.backendJobTracker.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
import com.jobtracker.backendJobTracker.exception.ResourceNotFoundException;
import com.jobtracker.backendJobTracker.interview.InterviewPrepService;
import com.jobtracker.backendJobTracker.status.StatusCategory;
import com.jobtracker.backendJobTracker.status.StatusCategoryRepository;
import com.jobtracker.backendJobTracker.user.User;
import com.jobtracker.backendJobTracker.user.UserRepository;

/**
 * Unit tests for {@link ApplicationService} with Mockito. Status is now a
 * per-user StatusCategory; movement between statuses is free (no state machine).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApplicationServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private ApplicationStatusHistoryRepository statusHistoryRepository;
    @Mock private ApplicationMapper applicationMapper;
    @Mock private CompanyService companyService;
    @Mock private UserRepository userRepository;
    @Mock private JobPostingExtractionService extractionService;
    @Mock private JobBoardDetector jobBoardDetector;
    @Mock private InterviewPrepService interviewPrepService;
    @Mock private StatusCategoryRepository statusCategoryRepository;

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

    private StatusCategory statusOf(ApplicationStatus systemType) {
        StatusCategory s = new StatusCategory();
        s.setId(UUID.randomUUID());
        s.setName(systemType == null ? "Custom" : systemType.name());
        s.setColor("#000000");
        s.setSystemType(systemType);
        return s;
    }

    @Test
    @DisplayName("create: MANUAL source, resolves default SAVED status, writes null->Saved history")
    void create_manualSource_writesInitialHistory() {
        CreateApplicationRequest request = validCreateRequest();
        StatusCategory saved = statusOf(ApplicationStatus.SAVED);
        when(companyService.findOrCreate(userId, "Acme")).thenReturn(new Company());
        when(userRepository.getReferenceById(userId)).thenReturn(new User());
        when(statusCategoryRepository.findByUserIdAndSystemType(userId, ApplicationStatus.SAVED))
                .thenReturn(Optional.of(saved));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(new ApplicationResponse());

        service.create(userId, request);

        verify(companyService).findOrCreate(userId, "Acme");

        ArgumentCaptor<Application> appCaptor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(appCaptor.capture());
        Application app = appCaptor.getValue();
        assertThat(app.getSourceBoard()).isEqualTo(SourceBoard.MANUAL);
        assertThat(app.getStatus()).isSameAs(saved);
        assertThat(app.getAppliedAt()).isNull();

        ArgumentCaptor<ApplicationStatusHistory> hCaptor = ArgumentCaptor.forClass(ApplicationStatusHistory.class);
        verify(statusHistoryRepository).save(hCaptor.capture());
        ApplicationStatusHistory history = hCaptor.getValue();
        assertThat(history.getFromLabel()).isNull();
        assertThat(history.getToLabel()).isEqualTo("SAVED");
    }

    @Test
    @DisplayName("create with an APPLIED-type status sets appliedAt")
    void create_appliedStatus_setsAppliedAt() {
        CreateApplicationRequest request = validCreateRequest();
        UUID statusId = UUID.randomUUID();
        request.setStatusId(statusId);
        StatusCategory applied = statusOf(ApplicationStatus.APPLIED);
        when(companyService.findOrCreate(userId, "Acme")).thenReturn(new Company());
        when(userRepository.getReferenceById(userId)).thenReturn(new User());
        when(statusCategoryRepository.findByIdAndUserId(statusId, userId)).thenReturn(Optional.of(applied));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(new ApplicationResponse());

        service.create(userId, request);

        ArgumentCaptor<Application> appCaptor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(appCaptor.capture());
        assertThat(appCaptor.getValue().getStatus()).isSameAs(applied);
        assertThat(appCaptor.getValue().getAppliedAt()).isNotNull();
    }

    @Test
    @DisplayName("updateStatus: any target allowed, APPLIED-type sets appliedAt, history uses labels")
    void updateStatus_movesFreely() {
        Application app = new Application();
        app.setStatus(statusOf(ApplicationStatus.SAVED));
        UUID targetId = UUID.randomUUID();
        StatusCategory target = statusOf(ApplicationStatus.APPLIED);
        when(applicationRepository.findByIdAndUserId(appId, userId)).thenReturn(Optional.of(app));
        when(statusCategoryRepository.findByIdAndUserId(targetId, userId)).thenReturn(Optional.of(target));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(new ApplicationResponse());

        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatusId(targetId);
        request.setNote("Sent CV");

        service.updateStatus(userId, appId, request);

        assertThat(app.getStatus()).isSameAs(target);
        assertThat(app.getAppliedAt()).isNotNull();

        ArgumentCaptor<ApplicationStatusHistory> hCaptor = ArgumentCaptor.forClass(ApplicationStatusHistory.class);
        verify(statusHistoryRepository).save(hCaptor.capture());
        ApplicationStatusHistory history = hCaptor.getValue();
        assertThat(history.getFromLabel()).isEqualTo("SAVED");
        assertThat(history.getToLabel()).isEqualTo("APPLIED");
        assertThat(history.getNote()).isEqualTo("Sent CV");
    }

    @Test
    @DisplayName("updateStatus into an INTERVIEW-type status triggers interview-prep creation")
    void updateStatus_interviewTriggersPrep() {
        Application app = new Application();
        app.setId(appId);
        app.setStatus(statusOf(ApplicationStatus.SCREENING));
        UUID targetId = UUID.randomUUID();
        when(applicationRepository.findByIdAndUserId(appId, userId)).thenReturn(Optional.of(app));
        when(statusCategoryRepository.findByIdAndUserId(targetId, userId))
                .thenReturn(Optional.of(statusOf(ApplicationStatus.INTERVIEW)));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(new ApplicationResponse());

        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatusId(targetId);

        service.updateStatus(userId, appId, request);

        verify(interviewPrepService).createIfNotExists(appId);
    }

    @Test
    @DisplayName("archive: soft delete sets archived=true and saves")
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
    @DisplayName("getById: found -> mapped to ApplicationResponse")
    void getById_found() {
        Application app = new Application();
        ApplicationResponse expected = new ApplicationResponse();
        when(applicationRepository.findByIdAndUserId(appId, userId)).thenReturn(Optional.of(app));
        when(applicationMapper.toResponse(app)).thenReturn(expected);

        assertThat(service.getById(userId, appId)).isSameAs(expected);
    }

    @Test
    @DisplayName("getById: missing/foreign application -> ResourceNotFoundException")
    void getById_notFound() {
        when(applicationRepository.findByIdAndUserId(eq(appId), eq(userId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(userId, appId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
