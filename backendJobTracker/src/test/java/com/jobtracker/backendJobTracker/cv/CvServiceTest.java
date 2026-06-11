package com.jobtracker.backendJobTracker.cv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.jobtracker.backendJobTracker.cv.dto.AddSkillRequest;
import com.jobtracker.backendJobTracker.cv.dto.CreateEducationRequest;
import com.jobtracker.backendJobTracker.cv.dto.CreateExperienceRequest;
import com.jobtracker.backendJobTracker.cv.dto.CreateProjectRequest;
import com.jobtracker.backendJobTracker.cv.dto.EducationResponse;
import com.jobtracker.backendJobTracker.cv.dto.ExperienceResponse;
import com.jobtracker.backendJobTracker.cv.dto.LanguageResponse;
import com.jobtracker.backendJobTracker.cv.dto.MasterCvResponse;
import com.jobtracker.backendJobTracker.cv.dto.ProjectResponse;
import com.jobtracker.backendJobTracker.cv.dto.SkillResponse;
import com.jobtracker.backendJobTracker.cv.dto.UpdateMasterCvRequest;
import com.jobtracker.backendJobTracker.cv.enums.SkillCategory;
import com.jobtracker.backendJobTracker.cv.enums.SkillLevel;
import com.jobtracker.backendJobTracker.cv.models.Education;
import com.jobtracker.backendJobTracker.cv.models.Experience;
import com.jobtracker.backendJobTracker.cv.models.MasterCv;
import com.jobtracker.backendJobTracker.cv.models.MasterCvSkill;
import com.jobtracker.backendJobTracker.cv.models.Project;
import com.jobtracker.backendJobTracker.cv.models.Skill;
import com.jobtracker.backendJobTracker.cv.repo.EducationRepository;
import com.jobtracker.backendJobTracker.cv.repo.ExperienceRepository;
import com.jobtracker.backendJobTracker.cv.repo.LanguageRepository;
import com.jobtracker.backendJobTracker.cv.repo.MasterCvRepository;
import com.jobtracker.backendJobTracker.cv.repo.MasterCvSkillRepository;
import com.jobtracker.backendJobTracker.cv.repo.ProjectRepository;
import com.jobtracker.backendJobTracker.cv.repo.SkillRepository;
import com.jobtracker.backendJobTracker.exception.ResourceNotFoundException;
import com.jobtracker.backendJobTracker.user.UserRepository;

/**
 * Unit-тести для {@link CvService} з Mockito — без БД і Spring контексту.
 * <p>
 * Перевіряємо ключові інваріанти сервісу: збір повного CV, PATCH-семантику
 * (null не затирає), tenant-safe finders (userId), нормалізацію скілів,
 * делегацію у {@link SkillService}.
 * <p>
 * УВАГА: реальний {@code getMy} НЕ робить auto-create — він кидає
 * {@link ResourceNotFoundException}, якщо CV відсутнє. Тести відображають
 * фактичну поведінку коду (а не auto-create з опису).
 */
@ExtendWith(MockitoExtension.class)
class CvServiceTest {

    @Mock private MasterCvRepository masterCvRepository;
    @Mock private ExperienceRepository experienceRepository;
    @Mock private EducationRepository educationRepository;
    @Mock private MasterCvSkillRepository masterCvSkillRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private LanguageRepository languageRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private UserRepository userRepository;
    @Mock private SkillService skillService;
    @Mock private CvMapper cvMapper;

    @InjectMocks private CvService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID cvId = UUID.randomUUID();

    private MasterCv existingCv() {
        MasterCv cv = new MasterCv();
        cv.setId(cvId);
        return cv;
    }

    // ─── getMy ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getMy: CV існує -> Response з усіма 5 колекціями зібраний з repos")
    void getMy_whenExists_returnsResponseWithAllCollections() {
        MasterCv cv = existingCv();
        when(masterCvRepository.findByUserId(userId)).thenReturn(Optional.of(cv));
        when(cvMapper.toMasterCvResponse(cv)).thenReturn(new MasterCvResponse());

        when(experienceRepository.findByMasterCvIdOrderByStartDateDesc(cvId))
                .thenReturn(List.of(new Experience()));
        when(cvMapper.toExperienceResponse(any())).thenReturn(new ExperienceResponse());

        when(educationRepository.findByMasterCvIdOrderByStartDateDesc(cvId))
                .thenReturn(List.of(new Education()));
        when(cvMapper.toEducationResponse(any())).thenReturn(new EducationResponse());

        when(masterCvSkillRepository.findByMasterCvId(cvId))
                .thenReturn(List.of(new MasterCvSkill()));
        when(cvMapper.toSkillResponse(any())).thenReturn(new SkillResponse());

        when(projectRepository.findByMasterCvIdOrderByStartDateDesc(cvId))
                .thenReturn(List.of(new Project()));
        when(cvMapper.toProjectResponse(any())).thenReturn(new ProjectResponse());

        when(languageRepository.findByMasterCvId(cvId))
                .thenReturn(List.of(new com.jobtracker.backendJobTracker.cv.models.Language()));
        when(cvMapper.toLanguageResponse(any())).thenReturn(new LanguageResponse());

        MasterCvResponse response = service.getMy(userId);

        assertThat(response.getExperiences()).hasSize(1);
        assertThat(response.getEducations()).hasSize(1);
        assertThat(response.getSkills()).hasSize(1);
        assertThat(response.getProjects()).hasSize(1);
        assertThat(response.getLanguages()).hasSize(1);
        // Tenant-safe вхідна точка: пошук CV завжди за userId.
        verify(masterCvRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("getMy: CV немає -> ResourceNotFoundException (БЕЗ auto-create)")
    void getMy_whenNotExists_throwsResourceNotFound() {
        when(masterCvRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMy(userId))
                .isInstanceOf(ResourceNotFoundException.class);

        // Нічого не створюється — getMy лише читає.
        verify(masterCvRepository, never()).save(any());
    }

    // ─── updateMetadata (PATCH semantics) ───────────────────────────────

    @Test
    @DisplayName("updateMetadata: null-поля НЕ затирають існуючі значення (PATCH)")
    void updateMetadata_nullFields_doNotOverwrite() {
        MasterCv cv = existingCv();
        cv.setFullName("Old Name");
        cv.setEmail("old@example.com");
        cv.setHeadline("Old headline");
        when(masterCvRepository.findByUserId(userId)).thenReturn(Optional.of(cv));
        when(masterCvRepository.save(any(MasterCv.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cvMapper.toMasterCvResponse(any(MasterCv.class))).thenReturn(new MasterCvResponse());

        UpdateMasterCvRequest request = new UpdateMasterCvRequest();
        request.setFullName("New Name"); // лише це поле; інші лишаються null

        service.updateMetadata(userId, request);

        ArgumentCaptor<MasterCv> captor = ArgumentCaptor.forClass(MasterCv.class);
        verify(masterCvRepository).save(captor.capture());
        MasterCv saved = captor.getValue();
        assertThat(saved.getFullName()).isEqualTo("New Name");      // оновлено
        assertThat(saved.getEmail()).isEqualTo("old@example.com");   // НЕ затерто
        assertThat(saved.getHeadline()).isEqualTo("Old headline");   // НЕ затерто
    }

    // ─── add* ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("addExperience: save викликається з полями з DTO та прив'язкою до CV")
    void addExperience_savesAndReturns() {
        MasterCv cv = existingCv();
        when(masterCvRepository.findByUserId(userId)).thenReturn(Optional.of(cv));
        when(experienceRepository.save(any(Experience.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cvMapper.toExperienceResponse(any(Experience.class))).thenReturn(new ExperienceResponse());

        CreateExperienceRequest request = new CreateExperienceRequest();
        request.setPosition("Backend Engineer");
        request.setCompany("Acme");
        request.setStartDate(LocalDate.of(2021, 1, 1));

        ExperienceResponse response = service.addExperience(userId, request);
        assertThat(response).isNotNull();

        ArgumentCaptor<Experience> captor = ArgumentCaptor.forClass(Experience.class);
        verify(experienceRepository).save(captor.capture());
        Experience saved = captor.getValue();
        assertThat(saved.getPosition()).isEqualTo("Backend Engineer");
        assertThat(saved.getCompany()).isEqualTo("Acme");
        assertThat(saved.getStartDate()).isEqualTo(LocalDate.of(2021, 1, 1));
        assertThat(saved.getMasterCv()).isSameAs(cv);
    }

    @Test
    @DisplayName("addEducation: save викликається, повертає Response")
    void addEducation_savesAndReturns() {
        MasterCv cv = existingCv();
        when(masterCvRepository.findByUserId(userId)).thenReturn(Optional.of(cv));
        when(educationRepository.save(any(Education.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cvMapper.toEducationResponse(any(Education.class))).thenReturn(new EducationResponse());

        CreateEducationRequest request = new CreateEducationRequest();
        request.setInstitution("WSB University");
        request.setDegree("Bachelor");

        assertThat(service.addEducation(userId, request)).isNotNull();

        ArgumentCaptor<Education> captor = ArgumentCaptor.forClass(Education.class);
        verify(educationRepository).save(captor.capture());
        assertThat(captor.getValue().getInstitution()).isEqualTo("WSB University");
        assertThat(captor.getValue().getMasterCv()).isSameAs(cv);
    }

    @Test
    @DisplayName("addProject: save викликається, повертає Response")
    void addProject_savesAndReturns() {
        MasterCv cv = existingCv();
        when(masterCvRepository.findByUserId(userId)).thenReturn(Optional.of(cv));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cvMapper.toProjectResponse(any(Project.class))).thenReturn(new ProjectResponse());

        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("JobTracker");
        request.setTechnologies("Java, Spring");

        assertThat(service.addProject(userId, request)).isNotNull();

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("JobTracker");
        assertThat(captor.getValue().getMasterCv()).isSameAs(cv);
    }

    // ─── delete / tenant safety ─────────────────────────────────────────

    @Test
    @DisplayName("deleteExperience: чужий userId -> ResourceNotFoundException, delete НЕ викликається")
    void deleteExperience_foreignUser_throwsAndDoesNotDelete() {
        UUID expId = UUID.randomUUID();
        // tenant-safe finder повертає empty -> запис або не існує, або належить іншому юзеру
        when(experienceRepository.findByIdAndMasterCv_User_Id(expId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteExperience(userId, expId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(experienceRepository).findByIdAndMasterCv_User_Id(expId, userId);
        verify(experienceRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteExperience: свій запис -> finder викликаний з (id, userId), delete викликається")
    void deleteExperience_own_usesTenantScopedFinder() {
        UUID expId = UUID.randomUUID();
        Experience exp = new Experience();
        when(experienceRepository.findByIdAndMasterCv_User_Id(expId, userId)).thenReturn(Optional.of(exp));

        service.deleteExperience(userId, expId);

        verify(experienceRepository).findByIdAndMasterCv_User_Id(expId, userId);
        verify(experienceRepository).delete(exp);
    }

    // ─── addSkill: нормалізація + делегація ─────────────────────────────

    @Test
    @DisplayName("addSkill: 'Java' -> existsByName('java') (lowercase+trim), делегує у findOrCreateSkill")
    void addSkill_normalizesNameForExistenceCheck_andDelegates() {
        MasterCv cv = existingCv();
        Skill skill = new Skill();
        skill.setId(UUID.randomUUID());
        skill.setName("java");

        when(masterCvRepository.findByUserId(userId)).thenReturn(Optional.of(cv));
        // Скіл уже у каталозі -> isNewSkill=false -> без afterCommit реєстрації (немає активної транзакції в unit-тесті)
        when(skillRepository.existsByName("java")).thenReturn(true);
        when(skillService.findOrCreateSkill("Java", SkillCategory.LANGUAGE)).thenReturn(skill);
        when(masterCvSkillRepository.findByMasterCvIdAndSkillId(cvId, skill.getId()))
                .thenReturn(Optional.empty());
        when(masterCvSkillRepository.save(any(MasterCvSkill.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cvMapper.toSkillResponse(any(MasterCvSkill.class))).thenReturn(new SkillResponse());

        AddSkillRequest request = new AddSkillRequest();
        request.setName("Java");
        request.setCategory(SkillCategory.LANGUAGE);
        request.setLevel(SkillLevel.ADVANCED);

        service.addSkill(userId, request);

        // Перевірка існування скіла нормалізована до lowercase.
        verify(skillRepository).existsByName("java");
        // Сама нормалізація у Skill (lowercase) — відповідальність SkillService (див. SkillServiceTest).
        verify(skillService).findOrCreateSkill("Java", SkillCategory.LANGUAGE);

        ArgumentCaptor<MasterCvSkill> captor = ArgumentCaptor.forClass(MasterCvSkill.class);
        verify(masterCvSkillRepository).save(captor.capture());
        MasterCvSkill saved = captor.getValue();
        assertThat(saved.getMasterCv()).isSameAs(cv);
        assertThat(saved.getSkill()).isSameAs(skill);
        assertThat(saved.getLevel()).isEqualTo(SkillLevel.ADVANCED);
    }
}
