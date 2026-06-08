package com.jobtracker.backendJobTracker.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.jobtracker.backendJobTracker.AbstractIntegrationTest;
import com.jobtracker.backendJobTracker.application.dto.ApplicationResponse;
import com.jobtracker.backendJobTracker.application.dto.CreateApplicationRequest;
import com.jobtracker.backendJobTracker.application.dto.UpdateStatusRequest;
import com.jobtracker.backendJobTracker.application.enums.ApplicationStatus;
import com.jobtracker.backendJobTracker.application.enums.ContractType;
import com.jobtracker.backendJobTracker.application.enums.Seniority;
import com.jobtracker.backendJobTracker.application.enums.WorkMode;
import com.jobtracker.backendJobTracker.user.User;

/**
 * IT для append-only audit log статусів (через service-шар + реальну БД).
 * <p>Потребує Docker (Testcontainers Postgres + Redis).
 */
class ApplicationStatusHistoryIT extends AbstractIntegrationTest {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ApplicationStatusHistoryRepository historyRepository;

    private CreateApplicationRequest createRequest() {
        CreateApplicationRequest r = new CreateApplicationRequest();
        r.setName("Dev");
        r.setCompanyName("Acme");
        r.setUrl("https://example.com/job/" + UUID.randomUUID());
        r.setContractType(ContractType.B2B);
        r.setSeniority(Seniority.MID);
        r.setWorkMode(WorkMode.REMOTE);
        return r;
    }

    @Test
    @DisplayName("create пише перший запис історії (from=null -> SAVED)")
    void createWritesInitialHistory() {
        User user = persistUser("hist1@example.com", "Passw0rd!");
        ApplicationResponse app = applicationService.create(user.getId(), createRequest());

        List<ApplicationStatusHistory> history =
                historyRepository.findByApplicationIdOrderByChangedAtAsc(app.getId());

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getFromStatus()).isNull();
        assertThat(history.get(0).getToStatus()).isEqualTo(ApplicationStatus.SAVED);
    }

    @Test
    @DisplayName("Кожен updateStatus додає рядок у хронологічному порядку")
    void eachTransitionAppendsRowInOrder() {
        User user = persistUser("hist2@example.com", "Passw0rd!");
        UUID userId = user.getId();
        ApplicationResponse app = applicationService.create(userId, createRequest());
        UUID appId = app.getId();

        applicationService.updateStatus(userId, appId, statusRequest(ApplicationStatus.APPLIED));
        applicationService.updateStatus(userId, appId, statusRequest(ApplicationStatus.SCREENING));

        List<ApplicationStatusHistory> history =
                historyRepository.findByApplicationIdOrderByChangedAtAsc(appId);

        assertThat(history).hasSize(3);
        assertThat(history).extracting(ApplicationStatusHistory::getToStatus)
                .containsExactly(ApplicationStatus.SAVED, ApplicationStatus.APPLIED, ApplicationStatus.SCREENING);
        assertThat(history).extracting(ApplicationStatusHistory::getFromStatus)
                .containsExactly(null, ApplicationStatus.SAVED, ApplicationStatus.APPLIED);
    }

    private UpdateStatusRequest statusRequest(ApplicationStatus status) {
        UpdateStatusRequest r = new UpdateStatusRequest();
        r.setStatus(status);
        return r;
    }
}
