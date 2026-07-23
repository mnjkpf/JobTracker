package com.jobtracker.backendJobTracker.coverletter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jobtracker.backendJobTracker.application.Application;
import com.jobtracker.backendJobTracker.application.ApplicationRepository;
import com.jobtracker.backendJobTracker.coverletter.dto.CoverLetterResponse;
import com.jobtracker.backendJobTracker.coverletter.dto.GenerateCoverLetterRequest;
import com.jobtracker.backendJobTracker.coverletter.dto.RefineCoverLetterRequest;
import com.jobtracker.backendJobTracker.coverletter.enums.CoverLetterTone;
import com.jobtracker.backendJobTracker.cv.models.MasterCv;
import com.jobtracker.backendJobTracker.cv.repo.MasterCvRepository;
import com.jobtracker.backendJobTracker.exception.BusinessRuleException;
import com.jobtracker.backendJobTracker.exception.ResourceNotFoundException;

/**
 * Unit-тести для {@link CoverLetterService} з Mockito.
 * <p>
 * Перевіряємо guards (немає CV / немає опису), версіонування (1, потім +1),
 * tone у entity, immutable refine (НОВИЙ запис, успадкований tone),
 * ownership-чеки та tenant-safe getById/delete.
 */
@ExtendWith(MockitoExtension.class)
class CoverLetterServiceTest {

    @Mock private CoverLetterRepository coverLetterRepository;
    @Mock private CoverLetterGenerator generator;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private MasterCvRepository masterCvRepository;
    @Mock private CoverLetterMapper mapper;

    @InjectMocks private CoverLetterService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID appId = UUID.randomUUID();
    private final UUID clId = UUID.randomUUID();

    private Application appWithDescription(String description) {
        Application app = new Application();
        app.setId(appId);
        app.setDescription(description);
        return app;
    }

    private GenerateCoverLetterRequest generateRequest(CoverLetterTone tone) {
        GenerateCoverLetterRequest r = new GenerateCoverLetterRequest();
        r.setTone(tone);
        r.setAdditionalInstructions("Mention remote work");
        return r;
    }

    // ─── generate guards ────────────────────────────────────────────────

    @Test
    @DisplayName("generate: немає CV -> BusinessRuleException, generator НЕ викликається")
    void generate_noCv_throws() {
        Application app = appWithDescription("Job description here");
        when(applicationRepository.findByIdAndUserId(appId, userId)).thenReturn(Optional.of(app));
        when(masterCvRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generate(userId, appId, generateRequest(CoverLetterTone.FORMAL)))
                .isInstanceOf(BusinessRuleException.class);

        verify(generator, never()).generate(any(), any(), any(), any());
        verify(coverLetterRepository, never()).save(any());
    }

    @Test
    @DisplayName("generate: немає опису вакансії -> BusinessRuleException (до fetch CV)")
    void generate_noDescription_throws() {
        Application app = appWithDescription("   ");
        when(applicationRepository.findByIdAndUserId(appId, userId)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> service.generate(userId, appId, generateRequest(CoverLetterTone.FORMAL)))
                .isInstanceOf(BusinessRuleException.class);

        verify(masterCvRepository, never()).findByUserId(any());
        verify(generator, never()).generate(any(), any(), any(), any());
    }

    // ─── versioning + tone ──────────────────────────────────────────────

    @Test
    @DisplayName("generate: перший лист -> version 1, tone з request у entity")
    void generate_firstVersion_setsToneAndVersionOne() {
        Application app = appWithDescription("Job description");
        when(applicationRepository.findByIdAndUserId(appId, userId)).thenReturn(Optional.of(app));
        when(masterCvRepository.findByUserId(userId)).thenReturn(Optional.of(new MasterCv()));
        when(generator.generate(any(), eq(app), eq(CoverLetterTone.ENTHUSIASTIC), any()))
                .thenReturn("Generated letter");
        when(coverLetterRepository.findTopByApplicationIdOrderByVersionDesc(appId)).thenReturn(Optional.empty());
        when(coverLetterRepository.save(any(CoverLetter.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(CoverLetter.class))).thenReturn(new CoverLetterResponse());

        service.generate(userId, appId, generateRequest(CoverLetterTone.ENTHUSIASTIC));

        ArgumentCaptor<CoverLetter> captor = ArgumentCaptor.forClass(CoverLetter.class);
        verify(coverLetterRepository).save(captor.capture());
        CoverLetter saved = captor.getValue();
        assertThat(saved.getVersion()).isEqualTo(1);
        assertThat(saved.getTone()).isEqualTo(CoverLetterTone.ENTHUSIASTIC);
        assertThat(saved.getContent()).isEqualTo("Generated letter");
        assertThat(saved.getApplication()).isSameAs(app);
    }

    @Test
    @DisplayName("generate: вже є version 2 -> наступна version 3 (latest+1)")
    void generate_nextVersion_isLatestPlusOne() {
        Application app = appWithDescription("Job description");
        CoverLetter latest = new CoverLetter();
        latest.setVersion(2);
        when(applicationRepository.findByIdAndUserId(appId, userId)).thenReturn(Optional.of(app));
        when(masterCvRepository.findByUserId(userId)).thenReturn(Optional.of(new MasterCv()));
        when(generator.generate(any(), any(), any(), any())).thenReturn("Letter");
        when(coverLetterRepository.findTopByApplicationIdOrderByVersionDesc(appId)).thenReturn(Optional.of(latest));
        when(coverLetterRepository.save(any(CoverLetter.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(CoverLetter.class))).thenReturn(new CoverLetterResponse());

        service.generate(userId, appId, generateRequest(CoverLetterTone.FORMAL));

        ArgumentCaptor<CoverLetter> captor = ArgumentCaptor.forClass(CoverLetter.class);
        verify(coverLetterRepository).save(captor.capture());
        assertThat(captor.getValue().getVersion()).isEqualTo(3);
    }

    // ─── refine ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("refine: створює НОВИЙ запис (не модифікує оригінал), успадковує tone, version latest+1")
    void refine_createsNewVersion_inheritsTone() {
        Application app = appWithDescription("Job description");
        CoverLetter original = new CoverLetter();
        original.setId(clId);
        original.setApplication(app);
        original.setContent("Original content");
        original.setTone(CoverLetterTone.CONVERSATIONAL);
        original.setVersion(1);

        when(coverLetterRepository.findByIdAndApplication_User_Id(clId, userId))
                .thenReturn(Optional.of(original));
        when(generator.refine("Original content", "make it shorter")).thenReturn("Refined content");
        when(coverLetterRepository.findTopByApplicationIdOrderByVersionDesc(appId))
                .thenReturn(Optional.of(original));
        when(coverLetterRepository.save(any(CoverLetter.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(CoverLetter.class))).thenReturn(new CoverLetterResponse());

        RefineCoverLetterRequest request = new RefineCoverLetterRequest();
        request.setInstruction("make it shorter");

        service.refine(userId, appId, clId, request);

        ArgumentCaptor<CoverLetter> captor = ArgumentCaptor.forClass(CoverLetter.class);
        verify(coverLetterRepository).save(captor.capture());
        CoverLetter saved = captor.getValue();
        assertThat(saved).isNotSameAs(original);                       // НОВИЙ запис
        assertThat(saved.getContent()).isEqualTo("Refined content");
        assertThat(saved.getTone()).isEqualTo(CoverLetterTone.CONVERSATIONAL); // успадковано
        assertThat(saved.getVersion()).isEqualTo(2);                   // latest(1)+1
        // Оригінал не чіпається.
        assertThat(original.getContent()).isEqualTo("Original content");
    }

    // ─── list / ownership ───────────────────────────────────────────────

    @Test
    @DisplayName("list: перевіряє ownership заявки (fetchOwnedApplication)")
    void list_checksOwnership() {
        when(applicationRepository.findByIdAndUserId(appId, userId))
                .thenReturn(Optional.of(appWithDescription("desc")));
        when(coverLetterRepository.findByApplicationIdOrderByVersionDesc(appId)).thenReturn(List.of());

        service.list(userId, appId);

        verify(applicationRepository).findByIdAndUserId(appId, userId);
    }

    @Test
    @DisplayName("list: чужа/відсутня заявка -> ResourceNotFoundException")
    void list_foreignApplication_throws() {
        when(applicationRepository.findByIdAndUserId(appId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.list(userId, appId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(coverLetterRepository, never()).findByApplicationIdOrderByVersionDesc(any());
    }

    // ─── getById / delete tenant safety ─────────────────────────────────

    @Test
    @DisplayName("getById: чужий юзер -> ResourceNotFoundException")
    void getById_foreignUser_throws() {
        when(coverLetterRepository.findByIdAndApplication_User_Id(clId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(userId, clId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("delete: tenant-safe finder (id, userId); знайдено -> delete викликається")
    void delete_tenantSafe() {
        CoverLetter cl = new CoverLetter();
        when(coverLetterRepository.findByIdAndApplication_User_Id(clId, userId)).thenReturn(Optional.of(cl));

        service.delete(userId, clId);

        verify(coverLetterRepository).findByIdAndApplication_User_Id(clId, userId);
        verify(coverLetterRepository).delete(cl);
    }

    @Test
    @DisplayName("delete: чужий юзер -> ResourceNotFoundException, delete НЕ викликається")
    void delete_foreignUser_throws() {
        when(coverLetterRepository.findByIdAndApplication_User_Id(clId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(userId, clId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(coverLetterRepository, never()).delete(any());
    }
}
