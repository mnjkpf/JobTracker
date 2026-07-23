package com.jobtracker.backendJobTracker.cv.tailored;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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
import com.jobtracker.backendJobTracker.cv.enums.CvLanguage;
import com.jobtracker.backendJobTracker.cv.models.MasterCv;
import com.jobtracker.backendJobTracker.cv.repo.MasterCvRepository;
import com.jobtracker.backendJobTracker.cv.tailored.dto.GenerateTailoredCvRequest;
import com.jobtracker.backendJobTracker.cv.tailored.dto.ParsedTailoredCv;
import com.jobtracker.backendJobTracker.cv.tailored.dto.ParsedTailoredExperience;
import com.jobtracker.backendJobTracker.cv.tailored.dto.ParsedTailoredSkill;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredCvResponse;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredEducationResponse;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredExperienceResponse;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredLanguageResponse;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredProjectResponse;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredSkillResponse;
import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredCv;
import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredEducation;
import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredExperience;
import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredLanguage;
import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredProject;
import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredSkill;
import com.jobtracker.backendJobTracker.cv.tailored.repos.TailoredCvRepository;
import com.jobtracker.backendJobTracker.cv.tailored.repos.TailoredEducationRepository;
import com.jobtracker.backendJobTracker.cv.tailored.repos.TailoredExperienceRepository;
import com.jobtracker.backendJobTracker.cv.tailored.repos.TailoredLanguageRepository;
import com.jobtracker.backendJobTracker.cv.tailored.repos.TailoredProjectRepository;
import com.jobtracker.backendJobTracker.cv.tailored.repos.TailoredSkillRepository;
import com.jobtracker.backendJobTracker.exception.BusinessRuleException;
import com.jobtracker.backendJobTracker.exception.ResourceNotFoundException;

/**
 * Unit-тести для {@link TailoredCvService} з Mockito.
 * <p>
 * Покриваємо: guard (немає CV), версіонування, coalesce hero-block (parsed→master),
 * skip-правила в attach* (порожні поля / непарсувана дата), нормалізацію скілів,
 * parseDate (через attachExperiences), tenant-safe getById/delete та
 * loadFullResponse (усі 5 колекцій).
 */
@ExtendWith(MockitoExtension.class)
class TailoredCvServiceTest {

    @Mock private TailoredCvRepository tailoredCvRepository;
    @Mock private TailoredExperienceRepository experienceRepository;
    @Mock private TailoredEducationRepository educationRepository;
    @Mock private TailoredSkillRepository skillRepository;
    @Mock private TailoredProjectRepository projectRepository;
    @Mock private TailoredLanguageRepository languageRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private MasterCvRepository masterCvRepository;
    @Mock private TailoredCvGenerator generator;
    @Mock private TailoredCvMapper mapper;

    @InjectMocks private TailoredCvService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID appId = UUID.randomUUID();
    private final UUID tcvId = UUID.randomUUID();

    private Application ownedApp(String description) {
        Application app = new Application();
        app.setId(appId);
        app.setDescription(description);
        when(applicationRepository.findByIdAndUserId(appId, userId)).thenReturn(Optional.of(app));
        return app;
    }

    /** Стабимо save (присвоює id) + loadFullResponse (mapper.toResponse + 5 finders empty). */
    private void stubSaveAndLoad() {
        when(tailoredCvRepository.save(any(TailoredCv.class))).thenAnswer(inv -> {
            TailoredCv c = inv.getArgument(0);
            if (c.getId() == null) c.setId(UUID.randomUUID());
            return c;
        });
        when(mapper.toResponse(any(TailoredCv.class))).thenReturn(new TailoredCvResponse());
        when(experienceRepository.findByTailoredCv_IdOrderByStartDateDesc(any())).thenReturn(List.of());
        when(educationRepository.findByTailoredCv_IdOrderByStartDateDesc(any())).thenReturn(List.of());
        when(skillRepository.findByTailoredCv_Id(any())).thenReturn(List.of());
        when(projectRepository.findByTailoredCv_IdOrderByStartDateDesc(any())).thenReturn(List.of());
        when(languageRepository.findByTailoredCv_Id(any())).thenReturn(List.of());
    }

    private GenerateTailoredCvRequest request() {
        GenerateTailoredCvRequest r = new GenerateTailoredCvRequest();
        r.setEmphasisInstructions("emphasis Spring");
        return r;
    }

    private TailoredCv captureSaved() {
        ArgumentCaptor<TailoredCv> captor = ArgumentCaptor.forClass(TailoredCv.class);
        verify(tailoredCvRepository).save(captor.capture());
        return captor.getValue();
    }

    // ─── guards / versioning ────────────────────────────────────────────

    @Test
    @DisplayName("generate: немає master CV -> BusinessRuleException, generator НЕ викликається")
    void generate_noCv_throws() {
        ownedApp("Job description");
        when(masterCvRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generate(userId, appId, request()))
                .isInstanceOf(BusinessRuleException.class);

        verify(generator, never()).generate(any(), any(), any());
        verify(tailoredCvRepository, never()).save(any());
    }

    @Test
    @DisplayName("generate: перша версія -> 1; наступна -> latest+1")
    void generate_versioning() {
        Application app = ownedApp("Job description");
        MasterCv master = new MasterCv();
        when(masterCvRepository.findByUserId(userId)).thenReturn(Optional.of(master));
        when(generator.generate(eq(master), eq(app), any())).thenReturn(ParsedTailoredCv.builder().build());
        when(tailoredCvRepository.findTopByApplication_IdOrderByVersionDesc(appId)).thenReturn(Optional.empty());
        stubSaveAndLoad();

        service.generate(userId, appId, request());
        assertThat(captureSaved().getVersion()).isEqualTo(1);

        // Тепер вже є version 3 -> наступна 4.
        TailoredCv latest = new TailoredCv();
        latest.setVersion(3);
        when(tailoredCvRepository.findTopByApplication_IdOrderByVersionDesc(appId)).thenReturn(Optional.of(latest));

        service.generate(userId, appId, request());
        ArgumentCaptor<TailoredCv> captor = ArgumentCaptor.forClass(TailoredCv.class);
        verify(tailoredCvRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(1).getVersion()).isEqualTo(4);
    }

    // ─── coalesce hero-block ────────────────────────────────────────────

    @Test
    @DisplayName("generate: hero-block coalesce — parsed null -> fallback на master; parsed заданий -> parsed")
    void generate_coalesceHeroBlock() {
        Application app = ownedApp("Job description");
        MasterCv master = new MasterCv();
        master.setFullName("Master Name");
        master.setEmail("master@example.com");
        master.setSummary("Master summary");
        master.setLanguage(CvLanguage.EN);

        ParsedTailoredCv parsed = ParsedTailoredCv.builder()
                .headline("Tailored Headline") // заданий -> використовується
                .fullName(null)                // null -> master
                .email(null)                   // null -> master
                .build();

        when(masterCvRepository.findByUserId(userId)).thenReturn(Optional.of(master));
        when(generator.generate(eq(master), eq(app), any())).thenReturn(parsed);
        when(tailoredCvRepository.findTopByApplication_IdOrderByVersionDesc(appId)).thenReturn(Optional.empty());
        stubSaveAndLoad();

        service.generate(userId, appId, request());

        TailoredCv saved = captureSaved();
        assertThat(saved.getFullName()).isEqualTo("Master Name");        // fallback
        assertThat(saved.getEmail()).isEqualTo("master@example.com");     // fallback
        assertThat(saved.getSummary()).isEqualTo("Master summary");       // fallback
        assertThat(saved.getHeadline()).isEqualTo("Tailored Headline");   // parsed
        assertThat(saved.getLanguage()).isEqualTo(CvLanguage.EN);
    }

    // ─── attach* skip rules ─────────────────────────────────────────────

    @Test
    @DisplayName("attachExperiences: skip коли position/company порожні")
    void attachExperiences_skipBlankPositionOrCompany() {
        ParsedTailoredCv parsed = ParsedTailoredCv.builder()
                .experiences(List.of(
                        ParsedTailoredExperience.builder()
                                .position("Engineer").company("Acme").startDate("2021-01-01").build(),
                        ParsedTailoredExperience.builder() // blank position -> skip
                                .position("  ").company("Acme").startDate("2021-01-01").build(),
                        ParsedTailoredExperience.builder() // blank company -> skip
                                .position("Engineer").company(null).startDate("2021-01-01").build()))
                .build();
        stubGenerate(parsed);
        when(experienceRepository.save(any(TailoredExperience.class))).thenAnswer(inv -> inv.getArgument(0));

        service.generate(userId, appId, request());

        // Лише 1 валідний досвід збережено.
        verify(experienceRepository, org.mockito.Mockito.times(1)).save(any(TailoredExperience.class));
    }

    @Test
    @DisplayName("attachExperiences: skip коли startDate не парситься (NOT NULL у БД)")
    void attachExperiences_skipUnparseableStartDate() {
        ParsedTailoredCv parsed = ParsedTailoredCv.builder()
                .experiences(List.of(
                        ParsedTailoredExperience.builder()
                                .position("Engineer").company("Acme").startDate("garbage").build()))
                .build();
        stubGenerate(parsed);

        service.generate(userId, appId, request());

        verify(experienceRepository, never()).save(any());
    }

    @Test
    @DisplayName("attachSkills: name нормалізується у lowercase")
    void attachSkills_lowercaseName() {
        ParsedTailoredCv parsed = ParsedTailoredCv.builder()
                .skills(List.of(ParsedTailoredSkill.builder().name("  JaVa  ").build()))
                .build();
        stubGenerate(parsed);
        when(skillRepository.save(any(TailoredSkill.class))).thenAnswer(inv -> inv.getArgument(0));

        service.generate(userId, appId, request());

        ArgumentCaptor<TailoredSkill> captor = ArgumentCaptor.forClass(TailoredSkill.class);
        verify(skillRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("java");
    }

    @Test
    @DisplayName("parseDate (через attachExperiences): ISO / YYYY-MM / YYYY; endDate 'present' -> null")
    void parseDate_variousFormats() {
        ParsedTailoredCv parsed = ParsedTailoredCv.builder()
                .experiences(List.of(
                        ParsedTailoredExperience.builder()
                                .position("A").company("C").startDate("2022-03-15").endDate("present").build(),
                        ParsedTailoredExperience.builder()
                                .position("B").company("C").startDate("2022-03").endDate("2023-06").build(),
                        ParsedTailoredExperience.builder()
                                .position("D").company("C").startDate("2021").build()))
                .build();
        stubGenerate(parsed);
        when(experienceRepository.save(any(TailoredExperience.class))).thenAnswer(inv -> inv.getArgument(0));

        service.generate(userId, appId, request());

        ArgumentCaptor<TailoredExperience> captor = ArgumentCaptor.forClass(TailoredExperience.class);
        verify(experienceRepository, org.mockito.Mockito.times(3)).save(captor.capture());
        List<TailoredExperience> saved = captor.getAllValues();
        assertThat(saved.get(0).getStartDate()).isEqualTo(LocalDate.of(2022, 3, 15)); // ISO
        assertThat(saved.get(0).getEndDate()).isNull();                                // present -> null
        assertThat(saved.get(1).getStartDate()).isEqualTo(LocalDate.of(2022, 3, 1));  // YYYY-MM
        assertThat(saved.get(1).getEndDate()).isEqualTo(LocalDate.of(2023, 6, 1));
        assertThat(saved.get(2).getStartDate()).isEqualTo(LocalDate.of(2021, 1, 1));  // YYYY
    }

    /** Спільний happy-path стаб для attach-тестів (parsed від generator). */
    private void stubGenerate(ParsedTailoredCv parsed) {
        Application app = ownedApp("Job description");
        MasterCv master = new MasterCv();
        when(masterCvRepository.findByUserId(userId)).thenReturn(Optional.of(master));
        when(generator.generate(eq(master), eq(app), any())).thenReturn(parsed);
        when(tailoredCvRepository.findTopByApplication_IdOrderByVersionDesc(appId)).thenReturn(Optional.empty());
        stubSaveAndLoad();
    }

    // ─── getById / delete / loadFullResponse ────────────────────────────

    @Test
    @DisplayName("getById: чужий tailored CV -> ResourceNotFoundException")
    void getById_foreign_throws() {
        when(tailoredCvRepository.findByIdAndApplication_User_Id(tcvId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(userId, tcvId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("delete: tenant-safe (id, userId); знайдено -> delete; чужий -> ResourceNotFoundException")
    void delete_tenantSafe() {
        TailoredCv cv = new TailoredCv();
        when(tailoredCvRepository.findByIdAndApplication_User_Id(tcvId, userId)).thenReturn(Optional.of(cv));

        service.delete(userId, tcvId);
        verify(tailoredCvRepository).delete(cv);

        when(tailoredCvRepository.findByIdAndApplication_User_Id(tcvId, userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(userId, tcvId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("loadFullResponse (через getById): завантажує усі 5 колекцій")
    void getById_loadsAllFiveCollections() {
        TailoredCv cv = new TailoredCv();
        cv.setId(tcvId);
        when(tailoredCvRepository.findByIdAndApplication_User_Id(tcvId, userId)).thenReturn(Optional.of(cv));
        when(mapper.toResponse(cv)).thenReturn(new TailoredCvResponse());

        when(experienceRepository.findByTailoredCv_IdOrderByStartDateDesc(tcvId))
                .thenReturn(List.of(new TailoredExperience()));
        when(mapper.toExperienceResponse(any())).thenReturn(new TailoredExperienceResponse());
        when(educationRepository.findByTailoredCv_IdOrderByStartDateDesc(tcvId))
                .thenReturn(List.of(new TailoredEducation()));
        when(mapper.toEducationResponse(any())).thenReturn(new TailoredEducationResponse());
        when(skillRepository.findByTailoredCv_Id(tcvId)).thenReturn(List.of(new TailoredSkill()));
        when(mapper.toSkillResponse(any())).thenReturn(new TailoredSkillResponse());
        when(projectRepository.findByTailoredCv_IdOrderByStartDateDesc(tcvId))
                .thenReturn(List.of(new TailoredProject()));
        when(mapper.toProjectResponse(any())).thenReturn(new TailoredProjectResponse());
        when(languageRepository.findByTailoredCv_Id(tcvId)).thenReturn(List.of(new TailoredLanguage()));
        when(mapper.toLanguageResponse(any())).thenReturn(new TailoredLanguageResponse());

        TailoredCvResponse response = service.getById(userId, tcvId);

        assertThat(response.getExperiences()).hasSize(1);
        assertThat(response.getEducations()).hasSize(1);
        assertThat(response.getSkills()).hasSize(1);
        assertThat(response.getProjects()).hasSize(1);
        assertThat(response.getLanguages()).hasSize(1);
    }
}
